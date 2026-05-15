package me.mraxetv.beastwithdraw.managers;
import me.mraxetv.beastlib.BeastLib;
import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;
import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.lib.tgui.gui.components.util.SkullUtil;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.filemanager.FolderYaml;
import me.mraxetv.beastwithdraw.utils.Utils;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public abstract class AssetHandler<T extends Number> {

    private BeastWithdrawPlugin pl;
    private String id;
    private final String configName;
    private FolderYaml config;
    private Material material;
    private String nbtTag;
    private TreeMap<Integer, Integer> amountModels = new TreeMap<>(Collections.reverseOrder());
    private DecimalFormat decimalFormat;


    public AssetHandler(BeastWithdrawPlugin pl, String id) {
        this(pl, id, id + ".yml");
    }

    public AssetHandler(BeastWithdrawPlugin pl, String id, String fileName) {
        this.pl = pl;
        this.id = id;
        this.configName = fileName.endsWith(".yml") ? fileName.substring(0, fileName.length() - 4) : fileName;
        config = new FolderYaml(pl, "Withdraws", fileName);
        if (getConfig().contains("Settings.Item")) material = XMaterial.matchXMaterial(getConfig().getString("Settings.Item")).get().parseMaterial();
        else material = XMaterial.PAPER.parseMaterial();
        nbtTag = getConfig().getString("Settings.NBTKey");
        setFormat();

        if (!getConfig().getBoolean("Settings.CustomModel.AmountModelData.Enabled")) return;
        if (!getConfig().contains("Settings.CustomModel.AmountModelData.Ranges")) return;

        for (Map<?, ?> entry : getConfig().getMapList("Settings.CustomModel.AmountModelData.Ranges")) {
            Object min = entry.get("min");
            Object model = entry.get("model");
            if (!(min instanceof Number) || !(model instanceof Number)) {
                continue;
            }

            amountModels.put(((Number) min).intValue(), ((Number) model).intValue());
        }

    }

    protected void setFormat() {

        String[] parts = getConfig().getString("Settings.Locale", "en_US").split("_");

        Locale locale;
        if (parts.length == 1) {
            locale = new Locale(parts[0]);
        } else if (parts.length == 2) {
            locale = new Locale(parts[0], parts[1]);
        } else {
            locale = Locale.ENGLISH;
        }
        decimalFormat = new DecimalFormat(getConfig().getString("Settings.AmountFormat", "###,##0.00"),
                DecimalFormatSymbols.getInstance(locale));
        decimalFormat.setRoundingMode(RoundingMode.HALF_DOWN);
    }


    protected void setMaterial(Material m) {
        material = m;
    }

    public Material getMaterial() {
        return material;
    }


    public String formatNumber(double amount) {
        String formatted = decimalFormat.format(amount);

        if (formatted.endsWith(".00")) {
            return formatted.substring(0, formatted.length() - 3); // remove ".00"
        }

        return formatted;
    }


    public String formatAmount(double amount) {
        String formatted = decimalFormat.format(amount);

        if (formatted.endsWith(".00")) {
            return formatted.substring(0, formatted.length() - 3); // remove ".00"
        }

        return formatted;
    }


    public String formatNumber(int amount) {

        //add code to add %symbol-prefix%

        return decimalFormat.format(amount);
    }
/*    public String formatNumber(Number amount){

        return  decimalFormat.format(amount);
    }*/

    public String formatWithPreSuffix(double amount) {
        return getConfig().getString("Settings.Messages.Prefix", "") + formatNumber(amount) + getConfig().getString("Settings.Messages.Suffix", "");
    }

    public abstract T getBalance(Player p);

    protected abstract void withdrawAmountExact(Player p, T amount);

    protected abstract void depositAmountExact(Player p, T amount);

    protected abstract T convertAmount(double amount);

    public final double getBalanceAsDouble(Player p) {
        T balance = getBalance(p);
        return balance == null ? 0.0D : balance.doubleValue();
    }

    public final void withdrawAmount(Player p, double amount) {
        withdrawAmountExact(p, convertAmount(amount));
    }

    public final void depositAmount(Player p, double amount) {
        depositAmountExact(p, convertAmount(amount));
    }

    public YamlDocument getConfig() {
        return config.getConfig();
    }

    public abstract boolean isToBigAmount(double amount);


    public int getModelDataFor(double amount, YamlDocument config) {
        if (!config.isSection("Settings.CustomModel.AmountModelData")) {
            return config.getInt("Settings.CustomModel.DefaultModelData", 0);
        }
        Section section = config.getSection("Settings.CustomModel.AmountModelData");
        if (section == null || !section.getBoolean("Enabled")) {
            return config.getInt("Settings.CustomModel.DefaultModelData", 0);
        }
        List<Map<?, ?>> ranges = section.getMapList("Ranges");
        for (Map<?, ?> entry : ranges) {
            double min = ((Number) entry.get("min")).doubleValue();
            double max = entry.containsKey("max")
                    ? ((Number) entry.get("max")).doubleValue()
                    : Double.POSITIVE_INFINITY;
            if (amount >= min && amount <= max) {
                return ((Number) entry.get("model")).intValue();
            }
        }
        // Fallback to default
        return config.getInt("Settings.CustomModel.DefaultModelData", 0);
    }


    public ItemStack getItem(String owner, double value, int amount, boolean signed, double tax) {

        ItemStack item = new ItemStack(material, amount);

        if (getConfig().contains("Settings.Data")) {
            item.setDurability(getConfig().getInt("Settings.Data").shortValue());
        }
        ItemMeta meta = item.getItemMeta();

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) meta.setCustomModelData(getModelDataFor(value, getConfig()));

        //Set item to glow
        if (getConfig().getBoolean("Settings.Glow")) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        }
        //Player Lore and Name of item
        String n;
        if (signed) {
            n = getConfig().getString("Settings.Player.Name");
            n = n.replace("%player%", owner);
            n = n.replace("%amount%", formatWithPreSuffix(value));
            n = n.replace("%level-amount%", formatNumber(XpManager.getLevelFromExp((int)value)));
            meta.setDisplayName(Utils.setColor(n));
            List<String> lore = new ArrayList<>();

            for (String s : getFormattedLore(getConfig().getStringList("Settings.Player.Lore"),owner,value,tax)) {
                s = s.replace("%player%", owner);
                s = s.replace("%amount%", formatWithPreSuffix(value));
                s = s.replace("%level-amount%", formatNumber(XpManager.getLevelFromExp((int)value)));
                if (s.contains("%tax%") && tax == 0) continue;
                s = Utils.setColor(s);
                lore.add(s);
            }
            //Set tax variable
            meta.setLore(lore);

        }
        //Server Lore and Name of Item
        else {
            n = getConfig().getString("Settings.Server.Name");
            n = n.replace("%amount%", formatWithPreSuffix(value));
            n = n.replace("%level-amount%", formatNumber(XpManager.getLevelFromExp((int)value)));
            meta.setDisplayName(Utils.setColor(n));
            List<String> lore = new ArrayList<String>();
            for (String s : getFormattedLore(getConfig().getStringList("Settings.Server.Lore"),owner,value,tax)) {
                s = s.replace("%amount%", formatWithPreSuffix(value));
                s = s.replace("%level-amount%", formatNumber(XpManager.getLevelFromExp((int)value)));
                if (s.contains("%tax%") && tax == 0) continue;
                s = Utils.setColor(s);
                lore.add(s);
            }
            meta.setLore(lore);

        }
        //Setting  item  flags
        setFlags(meta, getConfig().getStringList("Settings.Flags"));

        item.setItemMeta(meta);
        NBTItem tag = new NBTItem(item);
        tag.setDouble(nbtTag, value);
        tag.setBoolean("bCraft", true);
        tag.setString("RedeemType", id);
        tag.setBoolean("Signed", signed);
        if (signed && owner != null && !owner.isEmpty()) {
            tag.setString("Signer", owner);
        }
        if (tax > 0) {
            tag.setDouble("Tax", tax);
        }
        item = tag.getItem();

        if (SkullUtil.isPlayerSkull(item) && getConfig().contains("Settings.SkullTexture")) {
            String skullTexture = getConfig().getString("Settings.SkullTexture");
            skullTexture = skullTexture.replace("%player%", owner);
            //Check if  texture value was  not fetched  yet  on server and executes fetching
            if (!BeastLib.getInstance().getUtils().hasFetchedHeadTexture(skullTexture)) {
                BeastLib.getInstance().getUtils().fetchHeadTexture(skullTexture);
            }
            //Applays skin finally to item stack
            BeastLib.getInstance().getUtils().setHeadTexture(item, skullTexture);
        }


        return item;

    }

    private List<String> getFormattedLore(List<String> baseLore, String owner, double value, double tax) {
        if(tax <= 0)  return baseLore;
        List<String> formatted = new ArrayList<>();
        List<String> taxLore = getConfig().getStringList("Settings.Tax.Lore");

        for (String line : baseLore) {
            if (line.contains("%tax%")) {
                for (String taxLine : taxLore) {
                    taxLine = taxLine.replace("%player%", owner);
                    taxLine = taxLine.replace("%amount%", formatWithPreSuffix(value));
                    taxLine = taxLine.replace("%tax%", formatTax(tax));
                    formatted.add(taxLine);
                }
            } else {
                line = line.replace("%player%", owner);
                line = line.replace("%amount%", formatWithPreSuffix(value));
                line = line.replace("%tax%", formatTax(tax)); // just in case user has %tax% elsewhere
                formatted.add(line);
            }
        }
        return formatted;
    }

    private void setFlags(ItemMeta itemMeta, List<String> flags) {
        if (flags == null || flags.isEmpty()) return;
        for (String s : flags) itemMeta.addItemFlags(ItemFlag.valueOf(s.toUpperCase()));
    }


    private String formatTax(double tax) {
        return String.format("%.0f%%", tax);
    }


    public Section getMessageSection() {
        return getConfig().getSection("Settings.Messages");
    }

    public boolean hasAmountModels() {
        return amountModels.size() > 0;
    }

    public String getID() {
        return id;
    }

    public String getConfigName() {
        return configName;
    }

    public boolean isTransactionLoggingEnabled() {
        return getConfig().getBoolean("Settings.Logs.Enabled", true);
    }

    public boolean isSeparateLogFileEnabled() {
        return getConfig().getBoolean("Settings.Logs.SeparateFile.Enabled", false);
    }

    public double calculateTax(double takenAmount, ItemStack itemStack) {

        NBTItem nbtItem = new NBTItem(itemStack);
        double taxPercentage = nbtItem.getDouble("Tax");
        if (taxPercentage <= 0) return 0;

        //double percentage = getConfig().getDouble("Settings.Charges.Tax.Percentage");
        double tax = (takenAmount * (Math.min(taxPercentage, 100.0) / 100));

        return tax;
    }

    public abstract WithdrawCMD getWithdrawCMD();

    public String getNbtTag() {
        return nbtTag;
    }


}
