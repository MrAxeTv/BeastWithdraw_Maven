package me.mraxetv.beastwithdraw;


import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.commands.cashwithdraw.CashNoteCMD;
import me.mraxetv.beastwithdraw.commands.tokenwithdraw.BeastTokenNoteCMD;
import me.mraxetv.beastwithdraw.filemanager.FileYml;
import me.mraxetv.beastwithdraw.listener.BTokensNoteRedeemListener;
import me.mraxetv.beastwithdraw.listener.CancelCraftingListener;
import me.mraxetv.beastwithdraw.utils.*;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.mraxetv.beastwithdraw.Items.ItemManager;
import me.mraxetv.beastwithdraw.commands.AliasesRegistration;
import me.mraxetv.beastwithdraw.commands.admin.BeastWithdrawCMD;
import me.mraxetv.beastwithdraw.commands.xpbottle.XpBottleCMD;
import me.mraxetv.beastwithdraw.listener.CashNoteRedeemListener;
import me.mraxetv.beastwithdraw.listener.XpBottleRedeemListener;

public class BeastWithdrawPlugin extends JavaPlugin {

    public ItemManager itemManger;
    public YmlFile messages;
    public AliasesRegistration aliases;
    private Utils utils;
    private ConfigUtils configUtils;
    private FileYml fileYml;
    private static BeastWithdrawPlugin instance;
    private static Economy econ = null;
    private WithdrawManager withdrawManager;

    public void onEnable() {
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.disablePackageWarning();

        instance = this;
        setupEconomy();
        registerConfigs();
        configUtils = new ConfigUtils(this);
        utils = new Utils(this);
        withdrawManager = new WithdrawManager(this);
        itemManger = new ItemManager(this);
        aliases = new AliasesRegistration(this);
        registerCommands();
        registerEvents();


        new BeastUtils(this, "13896").getBVersion(version -> {
            if (getDescription().getVersion().equalsIgnoreCase(version)) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&4Beast&bWithdraw&7] &6There is not a new update available."));
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&4Beast&bWithdraw&7] &4There is a new update available."));
            }
        });

        //new ExtensionManager(this);

        int pluginId = 9409; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&4Beast&bWithdraw&7] &2Version " + getDescription().getVersion() + " : has been enabled!"));


    }

    public void reload() {
        HandlerList.unregisterAll(this);
        reloadConfig();
        registerConfigs();
        registerEvents();
        registerCommands();
    }


    public void registerConfigs() {
        fileYml = new FileYml(this, "config.yml");
        messages = new YmlFile(this, "messages.yml");
        messages.saveDeafultConfig();
    }

    public void registerCommands() {

        getCommand("BeastWithdraw").setExecutor(new BeastWithdrawCMD(this));

        if (getConfig().getBoolean("Settings.Withdraws.XpBottle.Enabled")) {
            getCommand("XpBottle").setExecutor(new XpBottleCMD(this));
        }
        if (getConfig().getBoolean("Settings.Withdraws.CashNote.Enabled")) {
            if ((getServer().getPluginManager().isPluginEnabled("Vault"))) {
                getCommand("bWithdraw").setExecutor(new CashNoteCMD(this));
            } else {
                getServer().getConsoleSender().sendMessage( "["+getDescription().getPrefix()+ "] Server is missing 'Vault' plugin which you need for economy(money) to work!");
            }
        }
        if (getConfig().getBoolean("Settings.Withdraws.BeastTokensNote.Enabled")) {
            if ((getServer().getPluginManager().isPluginEnabled("BeastTokens"))) {
                getCommand("btWithdraw").setExecutor(new BeastTokenNoteCMD(this));
            } else {
                utils.sendMessage( getServer().getConsoleSender(),"&4["+getDescription().getName()+ "] &cServer is missing 'BeastTokens' plugin which you need for 'Tokens Note' to work!");
            }
        }
        getAliasesManager().syncCommands();
    }


    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        if ((getServer().getPluginManager().isPluginEnabled("Vault"))) {
            pm.registerEvents(new CashNoteRedeemListener(this), this);
        }
        if (getConfig().getBoolean("Settings.Withdraws.BeastTokensNote.Enabled")) new BTokensNoteRedeemListener(this);
        if (getConfig().getBoolean("Settings.Withdraws.XpBottle.Enabled")) new XpBottleRedeemListener(this);
        if (getConfig().getBoolean("Settings.Withdraws.CashNote.Enabled")) new CancelCraftingListener(this);
    }


    public void onDisable() {
    }


    public FileConfiguration getMessages() {
        return messages.getConfig();
    }

    public AliasesRegistration getAliasesManager() {
        return aliases;
    }

    public Utils getUtils() {
        return utils;

    }

    private void setupEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        econ = rsp.getProvider();
    }

    public static Economy getEcon() {
        return econ;
    }

    public FileConfiguration getConfig() {
        return fileYml.getConfig();
    }

    public WithdrawManager getWithdrawManager() {
        return withdrawManager;
    }

    public ItemManager getItemManger() {
        return itemManger;
    }

    public static final BeastWithdrawPlugin getInstance() {
        return instance;
    }


}






