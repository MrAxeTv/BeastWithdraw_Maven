package me.mraxetv.beastwithdraw;


import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.filemanager.MessagesYml;

import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.bstats.bukkit.Metrics;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.commands.AliasesRegistration;
import me.mraxetv.beastwithdraw.commands.admin.BeastWithdrawCMD;
import me.mraxetv.beastwithdraw.filemanager.FileYml;
import me.mraxetv.beastwithdraw.listener.CancelCraftingListener;
import me.mraxetv.beastwithdraw.listener.DispenserXpBottleListener;
import me.mraxetv.beastwithdraw.managers.WithdrawManager;
import me.mraxetv.beastwithdraw.utils.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BeastWithdrawPlugin extends JavaPlugin implements BeastLibAPI {


    public MessagesYml messages;
    private Utils utils;
    private ConfigLang configLang;
    private FileYml fileYml;
    private static BeastWithdrawPlugin instance;
    private static Economy econ = null;
    private WithdrawManager withdrawManager;
    private Metrics metrics;


    private boolean setupBeastLib() {
        return getServer().getPluginManager().isPluginEnabled("BeastLib");
    }



    public void onEnable() {

        if (!setupBeastLib()) {

            //new BeastLogger(this);
            //new BeastLogger();
            new BukkitRunnable() {
                @Override
                public void run() {

                    //new BeastLogger();

                   // BeastLogger.log(Level.SEVERE, "is missing BeastCore dependency!");
                    getServer().getLogger().severe("[" + getDescription().getName() + "] is missing BeastLib dependency!");
                }
            }.runTaskLater(this,100);
            return;


        }
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.disablePackageWarning();

        instance = this;
        setupEconomy();
        registerConfigs();
        configLang = new ConfigLang(this);
        new MessagesLang(this);
        utils = new Utils(this);
        //currencyLogger = new CurrencyLogger(this);
        withdrawManager = new WithdrawManager(this);

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
        metrics = new Metrics(this, pluginId);

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
        messages = new MessagesYml(this);
        //messages.saveDeafultConfig();
    }

    public void registerCommands() {

        getCommand("BeastWithdraw").setExecutor(new BeastWithdrawCMD(this));

/*        if (getConfig().getBoolean("Settings.Withdraws.XpBottle.Enabled")) {
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
        }*/
        AliasesRegistration.syncCommands();
    }


    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
       // if ((getServer().getPluginManager().isPluginEnabled("Vault"))) {
           // pm.registerEvents(new CashNoteRedeemListener(this), this);
        //}
       // if (getConfig().getBoolean("Settings.Withdraws.BeastTokensNote.Enabled")) new BTokensNoteRedeemListener(this);
       // if (getConfig().getBoolean("Settings.Withdraws.XpBottle.Enabled")) new XpBottleRedeemListener(this);
        if (getConfig().getBoolean("Settings.Withdraws.CashNote.Enabled")) new CancelCraftingListener(this);
        new DispenserXpBottleListener(this);
    }


    public void onDisable() {
        //getServer().getPluginManager().

        if(metrics != null) metrics.shutdown();
    }


    public YamlDocument getMessages() {
        return messages.getConfig();
    }


    public Utils getUtils() {
        return utils;

    }

    private void setupEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;

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



    public static final BeastWithdrawPlugin getInstance() {
        return instance;
    }


    @Override
    public YamlDocument getMessagesYml() {
        return messages.getConfig();
    }
}






