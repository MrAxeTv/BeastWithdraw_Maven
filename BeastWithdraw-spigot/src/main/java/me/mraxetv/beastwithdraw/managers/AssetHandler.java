package me.mraxetv.beastwithdraw.managers;


import me.mraxetv.beastcore.utils.nbtapi.NBTItem;
import me.mraxetv.beastcore.utils.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.filemanager.FolderYaml;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.TreeMap;

public abstract class AssetHandler {

    private BeastWithdrawPlugin pl;
    private String id;
    private FolderYaml config;
    private Material material;
    private String nbtTag;
    private TreeMap<Integer, Integer> amountModels = new TreeMap<>(Collections.reverseOrder());
    private DecimalFormat decimalFormat;


    public AssetHandler(BeastWithdrawPlugin pl, String id) {
        this.pl = pl;
        this.id = id;
        config = new FolderYaml(pl, "Withdraws", id + ".yml");
        if(getConfig().isSet("Settings.Item")) material = Material.getMaterial(getConfig().getString("Settings.Item"));
        nbtTag = getConfig().getString("Settings.NBTLore");
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

        decimalFormat = new DecimalFormat(getConfig().getString("Settings.AmountFormat", "###,##0.##"), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    }

    protected void setMaterial(Material m){
        material = m;
    }

    public String getFormattedNumber(double amount){

        return  decimalFormat.format(amount);
    }

    public abstract double getBalance(Player p);

    public abstract void withdrawAmount(Player p, double amount);

    public abstract void depositAmount(Player p, double amount);

    public FileConfiguration getConfig() {
        return config.getConfig();
    }

    public ItemStack getItem(String owner, double value, int amount, boolean signed) {
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
            n = n.replaceAll("%amount%", "" + pl.getUtils().formatDouble(value));
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', n));
            ArrayList<String> lore = new ArrayList<String>();
            for (String s : getConfig().getStringList("Settings.Player.Lore")) {
                s = ChatColor.translateAlternateColorCodes('&', s);
                s = s.replace("%player%", "" + owner);
                s = s.replace("%amount%", "" + pl.getUtils().formatDouble(value));
                lore.add(s);
            }
            meta.setLore(lore);
        }
        //Server Lore and Name of Item
        else {
            n = getConfig().getString("Settings.Server.Name");
            n = n.replaceAll("%amount%", "" + pl.getUtils().formatDouble(value));
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', n));
            ArrayList<String> lore = new ArrayList<String>();
            for (String s : getConfig().getStringList("Settings.Server.Lore")) {
                s = ChatColor.translateAlternateColorCodes('&', s);
                s = s.replace("%amount%", "" + pl.getUtils().formatDouble(value));
                lore.add(s);
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        NBTItem tag = new NBTItem(item);
        tag.setDouble(nbtTag, value);
        tag.setBoolean("bCraft", true);
        item = tag.getItem();
        return item;

    }

    public boolean hasAmountModels() {
        return amountModels.size() > 0;
    }

    public String getID(){
        return id;
    }

}
