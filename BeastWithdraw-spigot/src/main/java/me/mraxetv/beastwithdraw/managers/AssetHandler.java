package me.mraxetv.beastwithdraw.managers;


import lombok.Getter;
import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.filemanager.FolderYaml;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public abstract class AssetHandler<T extends Number>  {

    private BeastWithdrawPlugin pl;
    private String id;
    private FolderYaml config;
    private Material material;
    @Getter
    private String nbtTag;
    private TreeMap<Integer, Integer> amountModels = new TreeMap<>(Collections.reverseOrder());
    private DecimalFormat decimalFormat;


    public AssetHandler(BeastWithdrawPlugin pl, String id) {
        //super(pl,id, new ArrayList<>(),"");
        this.pl = pl;
        this.id = id;
        config = new FolderYaml(pl, "Withdraws", id + ".yml");
        if(getConfig().isSet("Settings.Item")) material = XMaterial.matchXMaterial(getConfig().getString("Settings.Item")).get().parseMaterial();
        else material = XMaterial.PAPER.parseMaterial();
        nbtTag = getConfig().getString("Settings.NBTKey");
        setFormat();

        if (!getConfig().getBoolean("Settings.CustomModel.AmountModelData.Enabled")) return;
        if (!getConfig().isSet("Settings.CustomModel.AmountModelData.Range")) return;

        for (String s : getConfig().getStringList("Settings.CustomModel.AmountModelData.Range")) {
            String args[] = s.split("-");
            if (!Utils.isInt(args[0])) continue;
            String data[] = args[1].split(":");
            if (!Utils.isInt(data[1])) continue;
            amountModels.put(Integer.parseInt(args[0]), Integer.parseInt(data[1]));
        }

    }

    protected void setFormat() {

        decimalFormat = new DecimalFormat(getConfig().getString("Settings.AmountFormat", "###,##0.00"), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        decimalFormat.setRoundingMode(RoundingMode.HALF_DOWN);
    }


    protected void setMaterial(Material m){
        material = m;
    }

    public Material getMaterial() {
        return material;
    }

    public String formatNumber(double amount){

        return  decimalFormat.format(amount);
    }

    public String formatNumber(int amount){

        //add code to add %symbol-prefix%

        return  decimalFormat.format(amount);
    }
/*    public String formatNumber(Number amount){

        return  decimalFormat.format(amount);
    }*/

    public String formatWithPreSuffix(double amount){
        return getConfig().getString("Settings.Messages.Prefix","") + formatNumber(amount) + getConfig().getString("Settings.Messages.Suffix","");
    }

    public abstract Double getBalance(Player p);

    public abstract void withdrawAmount(Player p, Double amount);

    public abstract void depositAmount(Player p, Double amount);

    public FileConfiguration getConfig() {
        return config.getConfig();
    }

    public abstract  boolean isToBigAmount(double amount);

    public ItemStack getItem(String owner, double value, int amount, boolean signed, double tax) {
        ItemStack item = new ItemStack(material, amount);

        if (getConfig().isSet("Settings.Data")) {
            item.setDurability((short) getConfig().getInt("Settings.Data"));
        }
        ItemMeta meta = item.getItemMeta();

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) {
            if (hasAmountModels()) {
                for (int i : amountModels.keySet())
                    if (value >= i) {
                        meta.setCustomModelData(amountModels.get(i));
                        break;
                    }
            } else
                meta.setCustomModelData(getConfig().getInt("Settings.CustomModel.Data"));
        }
        //Set item to glow
        if (getConfig().getBoolean("Settings.Glow")) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        //Player Lore and Name of item
        String n;
        if (signed) {
            n = getConfig().getString("Settings.Player.Name");
            n = n.replaceAll("%player%", owner);
            n = n.replaceAll("%amount%", "" + formatNumber(value));
            meta.setDisplayName(Utils.setColor(n));
            List<String> lore = new ArrayList<>();
            for (String s : getConfig().getStringList("Settings.Player.Lore")) {
                s = s.replace("%player%", "" + owner);
                s = s.replace("%amount%", "" + formatNumber(value));
                if(s.contains("%tax%") && tax == 0) continue;
                s = Utils.setColor(s);
                lore.add(s);
            }
            //Set tax variable
            if(tax > 0) lore = getFormattedLore(lore, owner, value, tax);
            meta.setLore(lore);

        }
        //Server Lore and Name of Item
        else {
            n = getConfig().getString("Settings.Server.Name");
            n = n.replaceAll("%amount%", "" + formatNumber(value));
            meta.setDisplayName(Utils.setColor(n));
            List<String> lore = new ArrayList<String>();
            for (String s : getConfig().getStringList("Settings.Server.Lore")) {
                s = s.replace("%amount%", "" + formatNumber(value));
                if(s.contains("%tax%") && tax == 0) continue;
                s = Utils.setColor(s);
                lore.add(s);
            }
            if(tax > 0) lore = getFormattedLore(lore, owner, value, tax);
            meta.setLore(lore);

        }
        item.setItemMeta(meta);
        NBTItem tag = new NBTItem(item);
        tag.setDouble(nbtTag, value);
        tag.setBoolean("bCraft", true);
        tag.setString("RedeemType",id);
        if(tax > 0) {
            tag.setDouble("Tax",tax);
        }
        item = tag.getItem();
        return item;

    }

    private List<String> getFormattedLore(List<String> baseLore, String owner, double value, double tax) {
        List<String> formatted = new ArrayList<>();
        List<String> taxLore = getConfig().getStringList("Settings.Tax.Lore");

        for (String line : baseLore) {
            if (line.contains("%tax%")) {
                for (String taxLine : taxLore) {
                    taxLine = taxLine.replace("%player%", owner);
                    taxLine = taxLine.replace("%amount%", formatNumber(value));
                    taxLine = taxLine.replace("%tax%", formatTax(tax));
                    formatted.add(Utils.setColor(taxLine));
                }
            } else {
                line = line.replace("%player%", owner);
                line = line.replace("%amount%", formatNumber(value));
                line = line.replace("%tax%", formatTax(tax)); // just in case user has %tax% elsewhere
                formatted.add(Utils.setColor(line));
            }
        }
        return formatted;
    }
    private String formatTax(double tax) {
        return String.format("%.0f%%", tax);
    }


    public ConfigurationSection getMessageSection(){
       return getConfig().getConfigurationSection("Settings.Messages");
    }

    public boolean hasAmountModels() {
        return amountModels.size() > 0;
    }

    public String getID(){
        return id;
    }

    public double calculateTax(Player p, double takenAmount, ItemStack itemStack) {

        NBTItem nbtItem = new NBTItem(itemStack);
        double taxPercentage = nbtItem.getDouble("Tax");
        if (taxPercentage <= 0) return 0;

        //double percentage = getConfig().getDouble("Settings.Charges.Tax.Percentage");
        double tax = (takenAmount * (Math.min(taxPercentage, 100.0) / 100));

        return tax;
    }


}
