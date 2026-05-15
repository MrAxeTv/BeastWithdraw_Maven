package me.mraxetv.beastwithdraw;

import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.api.yaml.YamlFileOptions;
import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.bstats.bukkit.Metrics;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.utils.LegacyCommandUtils;
import me.mraxetv.beastwithdraw.commands.admin.BeastWithdrawCMD;
import me.mraxetv.beastwithdraw.filemanager.FileYml;
import me.mraxetv.beastwithdraw.listener.CancelCraftingListener;
import me.mraxetv.beastwithdraw.listener.DispenserXpBottleListener;
import me.mraxetv.beastwithdraw.listener.ItemDropListener;
import me.mraxetv.beastwithdraw.logging.WithdrawLogger;
import me.mraxetv.beastwithdraw.managers.WithdrawManager;
import me.mraxetv.beastwithdraw.utils.*;
import me.mraxetv.beastwithdraw.utils.updatechecker.UpdateManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BeastWithdrawPlugin extends JavaPlugin implements BeastLibAPI {

//Add short formated placeholder for large numbers like 1.2K, 3.4M, 5.6B etc. 11/2025
    private static final String DEFAULT_LANGUAGE_FILE = "en-US.yml";
    private YamlDocument messages;
    private Utils utils;
    private ConfigLang configLang;
    private MessagesLang messagesLang;
    private FileYml fileYml;
    private static BeastWithdrawPlugin instance;
    private static Economy econ = null;
    private WithdrawManager withdrawManager;
    private WithdrawLogger withdrawLogger;
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

        if( MinecraftVersion.isNewerThan(MinecraftVersion.MC26_1))
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
        withdrawLogger = new WithdrawLogger(this);
        withdrawManager = new WithdrawManager(this);

        registerCommands();
        registerEvents();
        //new UpdateManager(this);


        new BeastUtils(this, "13896").getBVersion(version -> {
            if (getDescription().getVersion().equalsIgnoreCase(version)) {
                //Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&4Beast&bWithdraw&7] &6There is not a new update available."));
            } else {
                //Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&4Beast&bWithdraw&7] &4There is a new update available."));
            }
        });

        //new ExtensionManager(this);

        int pluginId = 9409; // <-- Replace with the id of your plugin!
        metrics = new Metrics(this, pluginId);

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&4Beast&bWithdraw&7] &2Version " + getDescription().getVersion() + " : has been enabled!"));
    }

    public void reload() {
        HandlerList.unregisterAll(this);
        unregisterAdminCommand();
        if (withdrawManager != null) {
            withdrawManager.unregisterAssetHandlers();
        }
        if (withdrawLogger != null) {
            withdrawLogger.shutdown();
        }
        registerConfigs();
        configLang = new ConfigLang(this);
        messagesLang = new MessagesLang(this);
        utils = new Utils(this);
        withdrawLogger = new WithdrawLogger(this);
        withdrawManager = new WithdrawManager(this);
        registerEvents();
        registerCommands();
    }


    public void registerConfigs() {
        fileYml = new FileYml(this, "config.yml", true);
        migrateLegacyMessagesFile();
        messages = loadLanguageConfig();
    }

    public void registerCommands() {
        unregisterAdminCommand();
        //purgeKnownAdminCommands();

        beastWithdrawCMD = new BeastWithdrawCMD(this,"beastwithdraw","Main admin command for BeastWithdraw plugin","/BeastWithdraw help",getSettings().getStringList("Settings.Aliases"));


        if (MinecraftVersion.getVersion().equals(MinecraftVersion.MC1_8_R3)) {
            LegacyCommandUtils.applyLegacyCommandFix(this,beastWithdrawCMD);
        }

        //AliasesRegistration.syncCommands();
    }


    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        if (getSettings().getBoolean("Settings.Withdraws.CashNote.Enabled")) new CancelCraftingListener(this);
        new DispenserXpBottleListener(this);
        new ItemDropListener(this);
    }


    public void onDisable() {
        HandlerList.unregisterAll(this);
        unregisterAdminCommand();
        if (withdrawManager != null) {
            withdrawManager.unregisterAssetHandlers();
        }
        if (withdrawLogger != null) {
            withdrawLogger.shutdown();
        }
        if(metrics != null) metrics.shutdown();
    }

    private void unregisterAdminCommand() {
        if (beastWithdrawCMD == null) {
            return;
        }

        beastWithdrawCMD.unregister();
        beastWithdrawCMD = null;
    }

/*    @SuppressWarnings("unchecked")
    private void purgeKnownAdminCommands() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            removeKnownCommand(knownCommands, "beastwithdraw");
            removeKnownCommand(knownCommands, getDescription().getName().toLowerCase() + ":beastwithdraw");

            List<String> aliases = getSettings().getStringList("Settings.Aliases");
            for (String alias : aliases) {
                if (alias == null || alias.trim().isEmpty()) {
                    continue;
                }

                String normalized = alias.toLowerCase();
                removeKnownCommand(knownCommands, normalized);
                removeKnownCommand(knownCommands, getDescription().getName().toLowerCase() + ":" + normalized);
            }
        } catch (Exception ignored) {
        }
    }

    private void removeKnownCommand(Map<String, Command> knownCommands, String key) {
        Command command = knownCommands.remove(key);
        if (command instanceof BeastWithdrawCMD) {
            ((BeastWithdrawCMD) command).unregister();
        }
    }*/

    public YamlDocument getMessages() {
        return messages;

    }



    private void setupEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;

        econ = rsp.getProvider();
    }

    public YamlDocument getSettings() {
        return fileYml.getConfig();
    }

    @Override
    public Utils getUtils() {
        return utils;
    }

    public MessagesLang getMessagesLang() {
        return messagesLang;
    }

    public static BeastWithdrawPlugin getInstance() {
        return instance;
    }

    public static Economy getEcon() {
        return econ;
    }

    public WithdrawManager getWithdrawManager() {
        return withdrawManager;
    }

    public WithdrawLogger getWithdrawLogger() {
        return withdrawLogger;
    }

    private void migrateLegacyMessagesFile() {
        File legacyMessages = new File(getDataFolder(), "messages.yml");
        if (!legacyMessages.exists()) {
            return;
        }

        String languageFile = getSettings().getString("Language.Settings.File", DEFAULT_LANGUAGE_FILE);
        File languageTarget = new File(getDataFolder(), "Lang/" + languageFile);
        if (languageTarget.exists()) {
            return;
        }

        if (languageTarget.getParentFile() != null && !languageTarget.getParentFile().exists()) {
            languageTarget.getParentFile().mkdirs();
        }

        try {
            Files.move(legacyMessages.toPath(), languageTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
            getLogger().info("Migrated legacy messages.yml to Lang/" + languageFile);
        } catch (IOException moveException) {
            try {
                Files.copy(legacyMessages.toPath(), languageTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("Copied legacy messages.yml to Lang/" + languageFile);
            } catch (IOException copyException) {
                throw new IllegalStateException("Failed to migrate legacy messages.yml to Lang/" + languageFile, copyException);
            }
        }
    }

    private YamlDocument loadLanguageConfig() {
        String languageFile = getSettings().getString("Language.Settings.File", DEFAULT_LANGUAGE_FILE);
        String relativePath = "Lang/" + languageFile;
        File languageDiskFile = new File(getDataFolder(), relativePath);
        boolean bundledResourceExists = getResource(relativePath) != null;

        try {
            if (bundledResourceExists) {
                return getYamlFiles().load(
                        this,
                        YamlFileOptions.builder(relativePath)
                                .setResourcePath(relativePath)
                                .setAutoUpdate(true)
                                .setCreateFileIfMissing(true)
                                .setRequireDefaultResource(true)
                                .build()
                );
            }

            if (languageDiskFile.exists()) {
                return getYamlFiles().load(
                        this,
                        YamlFileOptions.builder(relativePath)
                                .setCreateFileIfMissing(true)
                                .build()
                );
            }

            getLogger().warning("Language file '" + languageFile + "' was not found. Falling back to " + DEFAULT_LANGUAGE_FILE + ".");
            return getYamlFiles().load(
                    this,
                    YamlFileOptions.builder("Lang/" + DEFAULT_LANGUAGE_FILE)
                            .setResourcePath("Lang/" + DEFAULT_LANGUAGE_FILE)
                            .setAutoUpdate(true)
                            .setCreateFileIfMissing(true)
                            .setRequireDefaultResource(true)
                            .build()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load language file " + languageFile, e);
        }
    }

}


