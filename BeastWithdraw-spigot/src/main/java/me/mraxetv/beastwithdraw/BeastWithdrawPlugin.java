package me.mraxetv.beastwithdraw;


import me.mraxetv.beastcore.utils.bstats.bukkit.Metrics;
import me.mraxetv.beastcore.utils.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.commands.cashwithdraw.CashNoteCMD;
import me.mraxetv.beastwithdraw.commands.tokenwithdraw.BeastTokenNoteCMD;
import me.mraxetv.beastwithdraw.filemanager.FileYml;
import me.mraxetv.beastwithdraw.listener.BTokensNoteRedeemListener;
import me.mraxetv.beastwithdraw.listener.CancelCraftingListener;
import me.mraxetv.beastwithdraw.managers.WithdrawManager;
import me.mraxetv.beastwithdraw.utils.*;
import net.milkbowl.vault.economy.Economy;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.mraxetv.beastwithdraw.Items.ItemManager;

import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;

public class BeastWithdrawPlugin extends JavaPlugin {

    public ItemManager itemManger;
    public YmlFile messages;
    private Utils utils;
    private CurrencyLogger currencyLogger;
    private ConfigLang configLang;
    private FileYml fileYml;
    private static BeastWithdrawPlugin instance;
    private static Economy econ = null;
    private WithdrawManager withdrawManager;

   // private static final Logger LOGGER = LogManager.getLogManager().getLogger("MyPlugin");
    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("com.yourcompany.logger");


    private boolean setupBeastCore() {
        return getServer().getPluginManager().isPluginEnabled("BeastCore");
    }



    public void onEnable() {

        Configurator.initialize(null, "log4j2.xml");

        // Initialize log4j
       // System.setProperty("log4j.configurationFile", getDataFolder().getAbsolutePath()+"\\log4j2.xml");

        // Initialize log4j system
        //System.setProperty("log4j.configurationFile", "log4j2.xml");

        // Log a message at the INFO level
       //LoggerContext context = Configurator.initialize(null, "log4j2.xml");

        //logger.info(""+context.getConfigLocation());
        logger.debug("This is a debug message.");
        logger.info("This is an info message.");
        logger.warn("This is a warning message.");
        logger.error("This is an error message.");
        logger.fatal("This is a fatal message.");
        logger.error("Your plugin is enabled!");
        //logger.severe("Your plugin has been enabled!");

       /* try {

            String path  = new File(getDataFolder(),"log4j2.xml").toURL().getPath();
            getLogger().warning(path);
            System.setProperty("log4j.configurationFile", path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }*/

       // System.setProperty("log4j.configurationFile", "file:///C:\\Users\\krist\\Desktop\\McServers\\spigot-1.19\\plugins\\BeastWithdraw\\log4j2.xml");
        System.out.println(getDataFolder().getAbsolutePath());
        getLogger().info("Server is on ");


        // Specify the path to your custom log4j2.xml configuration file


       // InputStream inputStream;
       // inputStream = getResource("log4j2.xml");

        // Create a ConfigurationSource object from the file


     /*   ConfigurationSource source = null;
        try {
            source = new ConfigurationSource(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        // Load the Log4j configuration from a resource file in the plugin JAR
       /* InputStream inputStream = getResource("log4j2.xml");
        if (inputStream == null) {
            System.err.println("Failed to load log4j2.xml");
        } else {
            System.out.println("log4j2.xml loaded successfully");
        }

        ConfigurationSource source = null;
        try {
            source = new ConfigurationSource(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

// Initialize Log4j with the configuration
        //LoggerContext context = LoggerContext.getContext(true);
        //context.setConfigLocation(source.getURI());

        //System.out.println("Current Log4j configuration file path: " + context.getConfigLocation());

        //LoggerContext context = (LoggerContext) LoggerContext.getContext(false);
        //String configLocation = context.getConfigLocation().getPath();
        //System.out.println("Log4j configuration file location: " + configLocation);








      /*  // Get the LoggerContext for the current application
        LoggerContext context = LoggerContext.getContext(false);

        // Reconfigure Log4j with the new configuration source
        Configurator.initialize(null, source);

        / Print the new configuration file path to the console
        System.out.println("Current Log4j configuration file path: " + context.getConfigLocation());




        // Set the logger implementation to Log4j
        System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "TRACE");
        // Get the server logger and use it with Log4j

        String e;

        //LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        String configLocation = config.getConfigurationSource().getLocation();
        System.out.println("Current Log4j configuration file path: " + configLocation);


        //Configurator.initialize(null, "log4j2.xml");
        Logger serverLogger = Bukkit.getLogger();
        //System.out.println("Log4j2 Configuration File Path: " + Configurator.;

        serverLogger.info("Hello, world!");


        Bukkit.getLogger().info("+9++ " +serverLogger.getName());
        serverLogger.info("Test");

        // Use the logger to log messages
        //LOGGER.info("Plugin enabled!");*/

        if (!setupBeastCore()) {

            //new BeastLogger(this);
            //new BeastLogger();
            new BukkitRunnable() {
                @Override
                public void run() {

                    //new BeastLogger();

                   // BeastLogger.log(Level.SEVERE, "is missing BeastCore dependency!");
                    //getServer().getLogger().severe("[" + getDescription().getName() + "] is missing BeastCore dependency!");
                }
            }.runTaskLater(this,2);
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
        itemManger = new ItemManager(this);
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
        //Metrics metrics = new Metrics(this, pluginId);
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

      /*  getCommand("BeastWithdraw").setExecutor(new BeastWithdrawCMD(this));

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
        getAliasesManager().syncCommands();*/
    }


    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        if ((getServer().getPluginManager().isPluginEnabled("Vault"))) {
          //  pm.registerEvents(new CashNoteRedeemListener(this), this);
        }
       // if (getConfig().getBoolean("Settings.Withdraws.BeastTokensNote.Enabled")) new BTokensNoteRedeemListener(this);
        //if (getConfig().getBoolean("Settings.Withdraws.XpBottle.Enabled")) new XpBottleRedeemListener(this);
        if (getConfig().getBoolean("Settings.Withdraws.CashNote.Enabled")) new CancelCraftingListener(this);
    }


    public void onDisable() {
        CurrencyLogger.close();
    }


    public FileConfiguration getMessages() {
        return messages.getConfig();
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






