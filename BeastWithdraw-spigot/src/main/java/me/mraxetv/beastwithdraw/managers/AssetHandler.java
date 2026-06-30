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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
    public static final String AMOUNT_OVERRIDE_NBT_TAG = "BeastWithdrawAmountOverride";
    private static final String WITHDRAW_CONFIG_FILE_NAME = "Withdraw.yml";
    private static final String DEPOSIT_CONFIG_FILE_NAME = "Depositor.yml";

    private BeastWithdrawPlugin pl;
    private String id;
    private final String configName;
    private final String configFolderName;
    private final String configFileName;
    private final String depositConfigFileName;
    private FolderYaml config;
    private FolderYaml depositConfig;
    private Material material;
    private String nbtTag;
    private DecimalFormat decimalFormat;


    public AssetHandler(BeastWithdrawPlugin pl, String id) {
        this(pl, id, id, WITHDRAW_CONFIG_FILE_NAME, id, WITHDRAW_CONFIG_FILE_NAME,
                "Withdraws/" + id + ".yml",
                "Withdraws/" + id + "/" + id + ".yml");
    }

    public AssetHandler(BeastWithdrawPlugin pl, String id, String fileName) {
        this(pl, id, getBaseName(fileName), WITHDRAW_CONFIG_FILE_NAME, getBaseName(fileName), WITHDRAW_CONFIG_FILE_NAME,
                "Withdraws/" + fileName,
                "Withdraws/" + getBaseName(fileName) + "/" + fileName);
    }

    public AssetHandler(BeastWithdrawPlugin pl, String id, String fileName, String resourceFileName) {
        this(pl, id, getBaseName(fileName), WITHDRAW_CONFIG_FILE_NAME, getBaseName(resourceFileName), WITHDRAW_CONFIG_FILE_NAME,
                "Withdraws/" + fileName,
                "Withdraws/" + getBaseName(fileName) + "/" + fileName,
                "Withdraws/" + resourceFileName,
                "Withdraws/" + getBaseName(resourceFileName) + "/" + resourceFileName);
    }

    public AssetHandler(BeastWithdrawPlugin pl, String id, String folderName, String fileName,
                        String resourceFolderName, String resourceFileName, String... legacyRelativePaths) {
        this.pl = pl;
        this.id = id;
        this.configName = sanitizeFolderName(folderName, getBaseName(fileName));
        this.configFolderName = sanitizeFolderName(folderName, this.configName);
        this.configFileName = fileName;
        this.depositConfigFileName = DEPOSIT_CONFIG_FILE_NAME;

        String resourceConfigName = resourceFileName == null || resourceFileName.trim().isEmpty() ? fileName : resourceFileName;
        String resourceConfigFolderName = sanitizeFolderName(resourceFolderName, getBaseName(resourceConfigName));
        String withdrawFolder = "Withdraws/" + this.configFolderName;
        String resourceFolder = "Withdraws/" + resourceConfigFolderName;

        config = new FolderYaml(pl, withdrawFolder, fileName, resourceFolder, resourceConfigName, legacyRelativePaths);
        String legacyDepositConfigName = this.configName + "-deposit-type.yml";
        depositConfig = new FolderYaml(
                pl,
                withdrawFolder,
                depositConfigFileName,
                resourceFolder,
                DEPOSIT_CONFIG_FILE_NAME,
                "Withdraws/" + legacyDepositConfigName,
                withdrawFolder + "/" + legacyDepositConfigName
        );
        migrateLegacyMisplacedFeeConfig();
        removeLegacyWithdrawGuiConfig();
        removeLegacyDepositorPermissionConfig();
        material = resolveMaterial(NoteItemSettings.resolve(getConfig(), 0).getItem());
        nbtTag = getConfig().getString("Settings.NBTKey");
        setFormat();

    }

    public AssetHandler(BeastWithdrawPlugin pl, String id, String folderName, String fileName, String depositFileName,
                        String resourceFolderName, String resourceFileName, String resourceDepositFileName,
                        String[] legacyRelativePaths) {
        this.pl = pl;
        this.id = id;
        this.configName = sanitizeFolderName(folderName, getBaseName(fileName));
        this.configFolderName = sanitizeFolderName(folderName, this.configName);
        this.configFileName = fileName;
        this.depositConfigFileName = depositFileName == null || depositFileName.trim().isEmpty()
                ? DEPOSIT_CONFIG_FILE_NAME
                : depositFileName;

        String resourceConfigName = resourceFileName == null || resourceFileName.trim().isEmpty() ? fileName : resourceFileName;
        String resourceDepositConfigName = resourceDepositFileName == null || resourceDepositFileName.trim().isEmpty()
                ? this.depositConfigFileName
                : resourceDepositFileName;
        String resourceConfigFolderName = sanitizeFolderName(resourceFolderName, getBaseName(resourceConfigName));
        String withdrawFolder = "Withdraws/" + this.configFolderName;
        String resourceFolder = "Withdraws/" + resourceConfigFolderName;

        config = new FolderYaml(pl, withdrawFolder, fileName, resourceFolder, resourceConfigName,
                legacyRelativePaths == null ? new String[0] : legacyRelativePaths);
        depositConfig = new FolderYaml(pl, withdrawFolder, this.depositConfigFileName, resourceFolder, resourceDepositConfigName);
        migrateLegacyMisplacedFeeConfig();
        removeLegacyWithdrawGuiConfig();
        removeLegacyDepositorPermissionConfig();
        material = resolveMaterial(NoteItemSettings.resolve(getConfig(), 0).getItem());
        nbtTag = getConfig().getString("Settings.NBTKey");
        setFormat();
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
        String currencySymbol = getConfig().getString("Settings.Messages.CurrencySymbol", "");
        String formattedAmount = formatNumber(amount);
        String rawAmount = String.valueOf(amount);
        String format = getConfig().getString("Settings.Messages.Format", null);
        if (format == null || format.trim().isEmpty()) format = "%symbol%%amount%";

        return format
                .replace("%symbol%", currencySymbol)
                .replace("%currency_symbol%", currencySymbol)
                .replace("%amount_raw%", rawAmount)
                .replace("%currency_raw%", rawAmount)
                .replace("%amount%", formattedAmount)
                .replace("%currency%", formattedAmount);
    }

    public List<String> getAliases() {
        return getConfig().getStringList("Settings.Aliases");
    }

    public String applyPlaceholders(String value, Player player) {
        if (value == null) {
            return null;
        }

        return value
                .replace("%type%", getConfigName())
                .replace("%type_id%", getID())
                .replace("%withdraw_command%", getCommandName());
    }

    public boolean hasWithdrawPermission(Player player) {
        return player != null && player.hasPermission("BeastWithdraw." + getID() + ".Withdraw");
    }

    public boolean hasWithdrawAllPermission(Player player) {
        return player != null && player.hasPermission("BeastWithdraw." + getID() + ".Withdraw.All");
    }

    public boolean hasBypassFeePermission(Player player) {
        return player != null && player.hasPermission("BeastWithdraw." + getID() + ".ByPass.Fee");
    }

    public boolean hasBypassTaxPermission(Player player) {
        return player != null && player.hasPermission("BeastWithdraw." + getID() + ".ByPass.Tax");
    }

    public boolean hasPermissionNote(Player player, String permissionName) {
        return player != null && permissionName != null
                && player.isPermissionSet("BeastWithdraw." + getID() + ".PermissionNotes." + permissionName);
    }

    public boolean hasRedeemPermission(Player player) {
        return player != null && player.hasPermission("BeastWithdraw." + getID() + ".Redeem");
    }

    public boolean hasStackedRedeemPermission(Player player) {
        return player != null && player.hasPermission("BeastWithdraw." + getID() + ".Redeem.Stacked");
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

    public YamlDocument getDepositConfig() {
        return depositConfig.getConfig();
    }

    public abstract boolean isToBigAmount(double amount);


    public ItemStack getItem(String owner, double value, int amount, boolean signed, double tax) {
        return getItem(owner, value, amount, signed, tax, null);
    }

    public ItemStack getItem(String owner, double value, int amount, boolean signed, double tax, String amountOverrideId) {
        String canonicalOverrideId = getCanonicalAmountOverrideId(amountOverrideId);
        NoteItemSettings.NoteVisual noteVisual = getNoteVisual(value, canonicalOverrideId);
        Material itemMaterial = resolveMaterial(noteVisual.getItem());

        ItemStack item = new ItemStack(itemMaterial, amount);

        if (noteVisual.isDataSet()) {
            item.setDurability((short) noteVisual.getData());
        }
        ItemMeta meta = item.getItemMeta();

        if (noteVisual.isCustomModelDataSet() && MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) {
            meta.setCustomModelData(Integer.valueOf(noteVisual.getCustomModelData()));
        }

        //Set item to glow
        if (noteVisual.isGlowEnabled()) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        }
        applyEnchants(meta, noteVisual.getEnchants());
        //Player Lore and Name of item
        String n;
        if (signed) {
            n = applyNotePlaceholders(noteVisual.getName(true), owner, value, tax);
            meta.setDisplayName(Utils.setColor(n));
            List<String> lore = new ArrayList<>();

            for (String s : getFormattedLore(noteVisual.getLore(true),owner,value,tax)) {
                if (s.contains("%tax%") && tax == 0) continue;
                s = applyNotePlaceholders(s, owner, value, tax);
                s = Utils.setColor(s);
                lore.add(s);
            }
            //Set tax variable
            meta.setLore(lore);

        }
        //Server Lore and Name of Item
        else {
            n = applyNotePlaceholders(noteVisual.getName(false), owner, value, tax);
            meta.setDisplayName(Utils.setColor(n));
            List<String> lore = new ArrayList<String>();
            for (String s : getFormattedLore(noteVisual.getLore(false),owner,value,tax)) {
                if (s.contains("%tax%") && tax == 0) continue;
                s = applyNotePlaceholders(s, owner, value, tax);
                s = Utils.setColor(s);
                lore.add(s);
            }
            meta.setLore(lore);

        }
        //Setting  item  flags
        setFlags(meta, noteVisual.getFlags());

        item.setItemMeta(meta);
        NBTItem tag = new NBTItem(item);
        tag.setDouble(getNbtTag(), value);
        tag.setBoolean("bCraft", true);
        tag.setString("RedeemType", id);
        if (canonicalOverrideId != null) {
            tag.setString(AMOUNT_OVERRIDE_NBT_TAG, canonicalOverrideId);
        }
        tag.setBoolean("Signed", signed);
        if (owner != null && !owner.isEmpty()) {
            tag.setString("Signer", owner);
        }
        if (tax > 0) {
            tag.setDouble("Tax", tax);
        }
        item = tag.getItem();

        if (SkullUtil.isPlayerSkull(item) && noteVisual.getSkullTexture() != null && !noteVisual.getSkullTexture().trim().isEmpty()) {
            String skullTexture = noteVisual.getSkullTexture();
            skullTexture = skullTexture.replace("%player%", owner == null ? "" : owner);
            //Check if  texture value was  not fetched  yet  on server and executes fetching
            if (!BeastLib.getInstance().getUtils().hasFetchedHeadTexture(skullTexture)) {
                BeastLib.getInstance().getUtils().fetchHeadTexture(skullTexture);
            }
            //Applays skin finally to item stack
            BeastLib.getInstance().getUtils().setHeadTexture(item, skullTexture);
        }


        return item;

    }

    public NoteItemSettings.NoteVisual getNoteVisual(double amount) {
        return getNoteVisual(amount, null);
    }

    public NoteItemSettings.NoteVisual getNoteVisual(double amount, String amountOverrideId) {
        return NoteItemSettings.resolve(getConfig(), amount, amountOverrideId);
    }

    public String getCustomNameFor(double amount) {
        return getCustomNameFor(amount, null);
    }

    public String getCustomNameFor(double amount, String amountOverrideId) {
        return getNoteVisual(amount, amountOverrideId).getCustomName();
    }

    public boolean isGlowEnabled(double amount) {
        return isGlowEnabled(amount, null);
    }

    public boolean isGlowEnabled(double amount, String amountOverrideId) {
        return getNoteVisual(amount, amountOverrideId).isGlowEnabled();
    }

    public String getGlowColor(double amount) {
        return getGlowColor(amount, null);
    }

    public String getGlowColor(double amount, String amountOverrideId) {
        return getNoteVisual(amount, amountOverrideId).getGlowColor();
    }

    public NoteItemSettings.SoundSettings getSoundSettings(String type, double amount) {
        return getSoundSettings(type, amount, null);
    }

    public NoteItemSettings.SoundSettings getSoundSettings(String type, double amount, String amountOverrideId) {
        NoteItemSettings.NoteVisual visual = getNoteVisual(amount, amountOverrideId);
        if ("Withdraw".equalsIgnoreCase(type)) return visual.getWithdrawSound();
        return visual.getRedeemSound();
    }

    public void playConfiguredSound(Player player, String type, double amount, String warningContext) {
        playConfiguredSound(player, type, amount, warningContext, null);
    }

    public void playConfiguredSound(Player player, String type, double amount, String warningContext, String amountOverrideId) {
        if (player == null) return;
        NoteItemSettings.SoundSettings soundSettings = getSoundSettings(type, amount, amountOverrideId);
        if (soundSettings == null || !soundSettings.isEnabled()) return;

        try {
            Sound sound = Sound.valueOf(soundSettings.getSound().toUpperCase());
            player.playSound(player.getLocation(), sound, soundSettings.getVolume(), soundSettings.getPitch());
        } catch (Exception exception) {
            Bukkit.getConsoleSender().sendMessage(
                    pl.getUtils().getPrefix() + "\u00A7cBroken sound in " + warningContext + " section!");
        }
    }

    public String applyNotePlaceholders(String value, String owner, double amount, double tax) {
        return applyPlaceholders(applyRawNotePlaceholders(value, owner, amount, tax), null);
    }

    public String applyRawNotePlaceholders(String value, String owner, double amount, double tax) {
        if (value == null) return "";
        String text = value.replace("%player%", owner == null ? "" : owner);
        text = text.replace("%amount%", formatWithPreSuffix(amount));
        text = text.replace("%amount_raw%", String.valueOf(amount));
        text = text.replace("%level-amount%", formatNumber(XpManager.getLevelFromExp((int) amount)));
        text = text.replace("%tax%", formatTax(tax));
        return text;
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
        for (String s : flags) {
            if (s == null || s.trim().isEmpty()) continue;
            try {
                itemMeta.addItemFlags(ItemFlag.valueOf(s.trim().toUpperCase().replace('-', '_').replace(' ', '_')));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void applyEnchants(ItemMeta itemMeta, Map<String, Integer> enchants) {
        if (itemMeta == null || enchants == null || enchants.isEmpty()) return;
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) continue;
            Enchantment enchantment = Enchantment.getByName(entry.getKey().trim().toUpperCase().replace('-', '_').replace(' ', '_'));
            if (enchantment == null) continue;
            int level = entry.getValue() == null ? 1 : Math.max(1, entry.getValue());
            itemMeta.addEnchant(enchantment, level, true);
        }
    }

    private Material resolveMaterial(String materialName) {
        if (materialName != null) {
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
            if (xMaterial.isPresent()) {
                Material resolved = xMaterial.get().parseMaterial();
                if (resolved != null) return resolved;
            }
        }
        Material fallback = XMaterial.PAPER.parseMaterial();
        return fallback == null ? Material.STONE : fallback;
    }


    private String formatTax(double tax) {
        return String.format("%.0f%%", tax);
    }


    public Section getMessageSection() {
        return getConfig().getSection("Settings.Messages");
    }

    public boolean hasAmountModels() {
        return getConfig().getBoolean("Settings.AmountOverrides.Enabled", false);
    }

    public List<String> getAmountOverrideIds() {
        return NoteItemSettings.getOverrideIds(getConfig());
    }

    public boolean hasAmountOverrideId(String amountOverrideId) {
        return NoteItemSettings.hasOverrideId(getConfig(), amountOverrideId);
    }

    public String getCanonicalAmountOverrideId(String amountOverrideId) {
        return NoteItemSettings.getCanonicalOverrideId(getConfig(), amountOverrideId);
    }

    public String getAmountOverrideId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;
        NBTItem nbtItem = new NBTItem(itemStack);
        if (!nbtItem.hasKey(AMOUNT_OVERRIDE_NBT_TAG)) return null;
        String overrideId = nbtItem.getString(AMOUNT_OVERRIDE_NBT_TAG);
        return overrideId == null || overrideId.trim().isEmpty() ? null : overrideId.trim();
    }

    public WithdrawItemRequirement getWithdrawItemRequirement() {
        return WithdrawItemRequirement.from(pl, this);
    }

    public String getID() {
        return id;
    }

    public String getCommandName() {
        return id;
    }

    public String getConfigName() {
        return configName;
    }

    public String getConfigFolderName() {
        return configFolderName;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public String getDepositConfigFileName() {
        return depositConfigFileName;
    }

    protected boolean isNewConfigFile() {
        return config.isNewFile();
    }

    protected boolean isNewDepositConfigFile() {
        return depositConfig.isNewFile();
    }

    protected void saveConfig() {
        config.saveConfig();
    }

    protected void saveDepositConfig() {
        depositConfig.saveConfig();
    }

    private void removeLegacyDepositorPermissionConfig() {
        if (depositConfig == null || depositConfig.getConfig() == null) {
            return;
        }
        if (!depositConfig.getConfig().contains("Command.Permission")) {
            return;
        }
        depositConfig.getConfig().remove("Command.Permission");
        depositConfig.saveConfig();
    }

    private void migrateLegacyMisplacedFeeConfig() {
        if (config == null || config.getConfig() == null) {
            return;
        }
        if (!config.getConfig().contains("Settings.PermissionNotes.Fee")) {
            return;
        }

        if (!config.getConfig().contains("Settings.Charges.Fee.Enabled")) {
            config.getConfig().set("Settings.Charges.Fee.Enabled",
                    config.getConfig().getBoolean("Settings.PermissionNotes.Fee.Enabled", false));
        }
        if (!config.getConfig().contains("Settings.Charges.Fee.Cost")) {
            config.getConfig().set("Settings.Charges.Fee.Cost",
                    config.getConfig().getDouble("Settings.PermissionNotes.Fee.Cost", 0D));
        }
        config.getConfig().remove("Settings.PermissionNotes.Fee");
        config.saveConfig();
    }

    private void removeLegacyWithdrawGuiConfig() {
        if (config == null || config.getConfig() == null) {
            return;
        }
        boolean changed = false;
        changed = removeConfigPath("WithdrawGUI.Command.Permission") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.AmountButtons") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.AmountEditor.Enabled") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.Material") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.Data") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.Amount") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.CustomModelData") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.DisplayName") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.Name") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.DisplayLore") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.Lore") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.Glow") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.Flags") || changed;
        changed = removeConfigPath("WithdrawGUI.GUI.Items.AmountItem.AmountOverrides") || changed;
        if (changed) {
            config.saveConfig();
        }
    }

    private boolean removeConfigPath(String path) {
        if (!config.getConfig().contains(path)) {
            return false;
        }

        config.getConfig().remove(path);
        return true;
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
        if (nbtTag == null || nbtTag.trim().isEmpty()) return id;
        return nbtTag;
    }

    private static String getBaseName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "Unknown";
        }

        String normalized = fileName.trim().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        if (normalized.toLowerCase(Locale.ENGLISH).endsWith(".yml")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.trim().isEmpty() ? "Unknown" : normalized.trim();
    }

    private static String sanitizeFolderName(String folderName, String fallback) {
        String value = folderName == null || folderName.trim().isEmpty() ? fallback : folderName.trim();
        value = value.replace('\\', '/');
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        value = value.replaceAll("[^A-Za-z0-9_-]", "");
        if (value.trim().isEmpty()) {
            value = fallback == null ? "Unknown" : fallback.replaceAll("[^A-Za-z0-9_-]", "");
        }
        return value.trim().isEmpty() ? "Unknown" : value;
    }


}
