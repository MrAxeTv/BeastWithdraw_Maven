package me.mraxetv.beastwithdraw;


import lombok.Getter;
import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.filemanager.MessagesYml;

import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.bstats.bukkit.Metrics;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.utils.BUtils;
import me.mraxetv.beastwithdraw.commands.admin.BeastWithdrawCMD;
import me.mraxetv.beastwithdraw.events.BTokensRedeemEvent;
import me.mraxetv.beastwithdraw.filemanager.FileYml;
import me.mraxetv.beastwithdraw.listener.CancelCraftingListener;
import me.mraxetv.beastwithdraw.listener.DispenserXpBottleListener;
import me.mraxetv.beastwithdraw.managers.WithdrawManager;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemEventFactory;
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

import java.util.HashMap;
import java.util.Map;

public class BeastWithdrawPlugin extends JavaPlugin implements BeastLibAPI {


    public MessagesYml messages;
    @Getter
    private Utils utils;
    private ConfigLang configLang;
    @Getter
    private MessagesLang messagesLang;
    private FileYml fileYml;
    @Getter
    private static BeastWithdrawPlugin instance;
    @Getter
    private static Economy econ = null;
    @Getter
    private WithdrawManager withdrawManager;
    private Metrics metrics;



    //Commands
    private BeastWithdrawCMD beastWithdrawCMD;
    private boolean setupBeastLib() {
        return getServer().getPluginManager().isPluginEnabled("BeastLib");
    }


    public void onEnable() {

        if (!setupBeastLib()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    getServer().getLogger().severe("[" + getDescription().getName() + "] is missing BeastLib dependency!");
                }
            }.runTaskLater(this,100);
            return;
        }
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.disablePackageWarning();

        if( MinecraftVersion.isNewerThan(MinecraftVersion.MC1_21_R5))
        {
            new BukkitRunnable() {
                @Override
                public void run() {
                    getServer().getLogger().warning("[" + getDescription().getName() + "] is not compatible with this minecraft version please do update to latest version!");
                }
            }.runTaskLater(this,100);

            return;
        }

        instance = this;
        setupEconomy();
        registerConfigs();
        configLang = new ConfigLang(this);
        messagesLang = new MessagesLang(this);
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

        beastWithdrawCMD = new BeastWithdrawCMD(this,"beastwithdraw","Main admin command for BeastWithdraw plugin","/BeastWithdraw help",getConfig().getStringList("Settings.Aliases"));

        //AliasesRegistration.syncCommands();
    }


    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        if (getConfig().getBoolean("Settings.Withdraws.CashNote.Enabled")) new CancelCraftingListener(this);
        new DispenserXpBottleListener(this);
    }


    public void onDisable() {
        if(metrics != null) metrics.shutdown();
    }

    public YamlDocument getMessages() {
        return messages.getConfig();

    }



    private void setupEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;

        econ = rsp.getProvider();
    }

    public FileConfiguration getConfig() {
        return fileYml.getConfig();
    }

}






