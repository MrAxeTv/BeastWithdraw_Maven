package me.mraxetv.beastwithdraw;

import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.api.yaml.YamlFileOptions;
import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;
import me.mraxetv.beastlib.lib.bstats.bukkit.Metrics;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.utils.LegacyCommandUtils;
import me.mraxetv.beastwithdraw.commands.DepositGuiCommand;
import me.mraxetv.beastwithdraw.commands.WithdrawGuiCommand;
import me.mraxetv.beastwithdraw.commands.admin.BeastWithdrawCMD;
import me.mraxetv.beastwithdraw.compatibility.BeastLibCompatibilityGuard;
import me.mraxetv.beastwithdraw.filemanager.FileYml;
import me.mraxetv.beastwithdraw.gui.depositor.DepositGuiManager;
import me.mraxetv.beastwithdraw.gui.depositor.DepositGuiProfile;
import me.mraxetv.beastwithdraw.gui.withdraw.WithdrawGuiManager;
import me.mraxetv.beastwithdraw.gui.withdraw.WithdrawGuiProfile;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BeastWithdrawPlugin extends JavaPlugin implements BeastLibAPI {

//Add short formated placeholder for large numbers like 1.2K, 3.4M, 5.6B etc. 11/2025
    private static final String DEFAULT_LANGUAGE_FILE = "en-US.yml";
    private static final String SPIGOT_USER_ID = "%%__USER__%%";
    private YamlDocument messages;
    private Utils utils;
    private ConfigLang configLang;
    private MessagesLang messagesLang;
    private FileYml fileYml;
    private FileYml depositSettingsYml;
    private FileYml withdrawSettingsYml;
    private static BeastWithdrawPlugin instance;
    private static Economy econ = null;
    private WithdrawManager withdrawManager;
    private WithdrawLogger withdrawLogger;
    private Metrics metrics;
    private UpdateManager updateManager;
    private BeastLibCompatibilityGuard beastLibCompatibilityGuard;
    private DepositGuiManager depositGuiManager;
    private WithdrawGuiManager withdrawGuiManager;



    //Commands
    private BeastWithdrawCMD beastWithdrawCMD;
    private final List<DepositGuiCommand> depositGuiCommands = new ArrayList<>();
    private final List<WithdrawGuiCommand> withdrawGuiCommands = new ArrayList<>();


    public void onEnable() {

        beastLibCompatibilityGuard = new BeastLibCompatibilityGuard(this);
        if (!beastLibCompatibilityGuard.isCompatible()) {
            instance = this;
            beastLibCompatibilityGuard.enableFallback();
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
        updateManager = new UpdateManager(this);


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
        shutdownUpdateManager();
        HandlerList.unregisterAll(this);
        unregisterAdminCommand();
        unregisterDepositCommand();
        unregisterWithdrawGuiCommand();
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
        updateManager = new UpdateManager(this);
    }


    public void registerConfigs() {
        fileYml = new FileYml(this, "config.yml", true);
        depositSettingsYml = new FileYml(this, "deposit-settings.yml", true);
        withdrawSettingsYml = new FileYml(this, "withdraw-settings.yml", true);
        migrateLegacyDepositorSettings();
        migrateDepositGuiDefaults();
        migrateWithdrawGuiLayoutDefaults();
        migrateLegacyMessagesFile();
        messages = loadLanguageConfig();
    }

    private void migrateWithdrawGuiLayoutDefaults() {
        double version = getWithdrawSettingsVersion();
        boolean changed = false;

        changed = removeWithdrawSettingsPath("Command.Permission") || changed;
        changed = removeWithdrawSettingsPath("GUI.AmountButtons") || changed;
        changed = removeWithdrawSettingsPath("GUI.AmountEditor.Enabled") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.Material") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.Data") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.Amount") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.CustomModelData") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.DisplayName") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.Name") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.DisplayLore") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.Lore") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.Glow") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.Flags") || changed;
        changed = removeWithdrawSettingsPath("GUI.Items.AmountItem.AmountOverrides") || changed;

        if (version >= 2.1D) {
            if (changed) {
                withdrawSettingsYml.saveConfig();
                getLogger().info("Removed obsolete withdraw GUI config.");
            }
            return;
        }

        if (version < 1.1D) {
            if (intListEquals(getWithdrawSettings().getIntList("Menu.TypeSlots"), 11, 13, 15, 21, 23, 25, 31, 33, 35)) {
                getWithdrawSettings().set("Menu.CenterTypeRows", true);
                getWithdrawSettings().set("Menu.TypeSlots", Arrays.asList(12, 14, 16, 21, 23, 25, 30, 32, 34));
                changed = true;
            } else {
                getWithdrawSettings().set("Menu.CenterTypeRows", false);
                changed = true;
            }

            changed = migrateWithdrawSlotIfUnchanged("GUI.Items.CloseButton.Slot", 50, 51) || changed;

            String fillerType = getWithdrawSettings().getString("GUI.EmptySlot.Pattern.Type", "");
            if ("BORDER".equalsIgnoreCase(fillerType)) {
                getWithdrawSettings().set("GUI.EmptySlot.Pattern.Type", "ALL");
                changed = true;
            }
        }

        if (version < 1.2D) {
            changed = migrateWithdrawSlotIfUnchanged("GUI.Items.InfoItem.Slot", 14, 5) || changed;
            getWithdrawSettings().set("ConfigVersion", 1.2D);
            changed = true;
        }

        if (version < 1.3D) {
            changed = migrateWithdrawStringIfUnchanged(
                    "GUI.Items.AddAmountButton.Material",
                    "LIGHT_BLUE_STAINED_GLASS_PANE",
                    "LIGHT_BLUE_TERRACOTTA"
            ) || changed;
            changed = migrateWithdrawStringIfUnchanged(
                    "GUI.Items.RemoveAmountButton.Material",
                    "RED_STAINED_GLASS_PANE",
                    "RED_TERRACOTTA"
            ) || changed;
            changed = migrateWithdrawStringIfUnchanged(
                    "GUI.Items.RemoveStackButton.Material",
                    "ORANGE_STAINED_GLASS_PANE",
                    "ORANGE_TERRACOTTA"
            ) || changed;
            changed = migrateWithdrawStringIfUnchanged(
                    "GUI.Items.AddStackButton.Material",
                    "LIME_STAINED_GLASS_PANE",
                    "LIME_TERRACOTTA"
            ) || changed;
            getWithdrawSettings().set("ConfigVersion", 1.3D);
            changed = true;
        }

        if (version < 1.4D) {
            getWithdrawSettings().set("ConfigVersion", 1.4D);
            changed = true;
        }

        if (version < 1.5D) {
            if (intListEquals(getWithdrawSettings().getIntList("Menu.McMMORedeemSkillMenu.SkillSlots"),
                    11, 12, 13, 14, 15, 16, 17,
                    20, 21, 22, 23, 24, 25, 26,
                    29, 30, 31, 32, 33, 34, 35,
                    38, 39, 40, 41, 42, 43, 44)) {
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.SkillSlots", Arrays.asList(
                        11, 12, 13, 14, 15, 16, 17,
                        20, 21, 22, 23, 24, 25, 26,
                        30, 34
                ));
                changed = true;
            }
            if (!getWithdrawSettings().contains("Menu.McMMORedeemSkillMenu.SkillOrder")) {
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.SkillOrder", Arrays.asList(
                        "Acrobatics", "Alchemy", "Archery", "Axes", "Crossbows", "Excavation", "Fishing",
                        "Herbalism", "Maces", "Mining", "Repair", "Swords", "Taming", "Tridents",
                        "Unarmed", "Woodcutting"
                ));
                changed = true;
            }

            changed = migrateWithdrawSlotIfUnchanged("Menu.McMMORedeemSkillMenu.Items.CreditButton.Slot", 5, 48) || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.Items.CreditButton.Material", "EMERALD", "NETHER_STAR") || changed;
            changed = migrateWithdrawSlotIfUnchanged("Menu.McMMORedeemSkillMenu.Items.BackButton.Slot", 49, 50) || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.Items.BackButton.Material", "ARROW", "EMERALD") || changed;
            changed = migrateWithdrawSlotIfUnchanged("Menu.McMMORedeemSkillMenu.Items.CloseButton.Slot", 51, 52) || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.Items.CloseButton.Material", "BARRIER", "PAPER") || changed;

            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Acrobatics", "FEATHER", "LEATHER_BOOTS") || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Axes", "IRON_AXE", "DIAMOND_AXE") || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Excavation", "IRON_SHOVEL", "DIAMOND_SHOVEL") || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Herbalism", "WHEAT", "FARMLAND") || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Maces", "MACE", "IRON_AXE") || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Mining", "IRON_PICKAXE", "DIAMOND_PICKAXE") || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Swords", "IRON_SWORD", "DIAMOND_SWORD") || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Unarmed", "LEATHER_CHESTPLATE", "STICK") || changed;
            changed = migrateWithdrawStringIfUnchanged("Menu.McMMORedeemSkillMenu.SkillIcons.Woodcutting", "IRON_AXE", "OAK_WOOD") || changed;

            if ("GREEN_STAINED_GLASS_PANE".equalsIgnoreCase(getWithdrawSettings().getString("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Dummy_1.Material", ""))
                    && "LIGHT_BLUE_STAINED_GLASS_PANE".equalsIgnoreCase(getWithdrawSettings().getString("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Dummy_2.Material", ""))) {
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Primary.Material", "CYAN_STAINED_GLASS_PANE");
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Primary.Slots", Arrays.asList(1, 2, 3, 4, 6, 7, 8, 9, 10, 18, 19, 27, 28, 36, 37, 45, 46, 47, 49, 51, 53, 54));
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Primary.DisplayName", " ");
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Primary.DisplayLore", new ArrayList<>());
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Primary.Flags", Arrays.asList("HIDE_ATTRIBUTES", "HIDE_POTION_EFFECTS"));
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Secondary.Material", "GRAY_STAINED_GLASS_PANE");
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Secondary.Slots", Arrays.asList(29, 31, 32, 33, 35, 38, 39, 40, 41, 42, 43, 44));
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Secondary.DisplayName", " ");
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Secondary.DisplayLore", new ArrayList<>());
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Secondary.Flags", Arrays.asList("HIDE_ATTRIBUTES", "HIDE_POTION_EFFECTS"));
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Accent.Material", "LIME_STAINED_GLASS_PANE");
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Accent.Slots", Arrays.asList(5, 48, 50, 52));
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Accent.DisplayName", " ");
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Accent.DisplayLore", new ArrayList<>());
                getWithdrawSettings().set("Menu.McMMORedeemSkillMenu.EmptySlot.Items.Accent.Flags", Arrays.asList("HIDE_ATTRIBUTES", "HIDE_POTION_EFFECTS"));
                changed = true;
            }

            getWithdrawSettings().set("ConfigVersion", 1.5D);
            changed = true;
        }

        if (version < 1.6D) {
            setWithdrawDefault("Sounds.Enabled", true);
            setWithdrawDefault("Sounds.Click", "UI_BUTTON_CLICK");
            setWithdrawDefault("Sounds.MenuOpen", "ENTITY_VILLAGER_TRADE");
            setWithdrawDefault("Sounds.Volume", 1D);
            setWithdrawDefault("Sounds.Pitch", 1D);

            setWithdrawDefault("Animations.Open.Enabled", false);
            setWithdrawDefault("Animations.Open.Type", "CENTER_OUT");
            setWithdrawDefault("Animations.Open.Triggers.MainMenu", true);
            setWithdrawDefault("Animations.Open.Triggers.McMMORedeemSkillMenu", true);
            setWithdrawDefault("Animations.Open.Triggers.AmountMenu", true);
            setWithdrawDefault("Animations.Open.StartDelayTicks", 0);
            setWithdrawDefault("Animations.Open.TickDelay", 1);
            setWithdrawDefault("Animations.Open.ItemsPerTick", 1);
            setWithdrawDefault("Animations.Open.RowPauseTicks", 1);
            setWithdrawDefault("Animations.Open.AnimateEmptySlots", false);
            setWithdrawDefault("Animations.Open.LockClicksUntilFinished", true);
            setWithdrawDefault("Animations.Open.Sounds.Enabled", true);
            setWithdrawDefault("Animations.Open.Sounds.Start", "");
            setWithdrawDefault("Animations.Open.Sounds.Item", "UI_BUTTON_CLICK");
            setWithdrawDefault("Animations.Open.Sounds.Row", "");
            setWithdrawDefault("Animations.Open.Sounds.Finish", "ENTITY_EXPERIENCE_ORB_PICKUP");
            setWithdrawDefault("Animations.Open.Sounds.StartVolume", 0.6D);
            setWithdrawDefault("Animations.Open.Sounds.StartPitch", 0.8D);
            setWithdrawDefault("Animations.Open.Sounds.ItemVolume", 0.45D);
            setWithdrawDefault("Animations.Open.Sounds.ItemPitch", 1.25D);
            setWithdrawDefault("Animations.Open.Sounds.ItemPitchStep", 0.03D);
            setWithdrawDefault("Animations.Open.Sounds.ItemPitchMax", 2.0D);
            setWithdrawDefault("Animations.Open.Sounds.RowVolume", 0.5D);
            setWithdrawDefault("Animations.Open.Sounds.RowPitch", 1.45D);
            setWithdrawDefault("Animations.Open.Sounds.FinishVolume", 0.7D);
            setWithdrawDefault("Animations.Open.Sounds.FinishPitch", 1.0D);
            setWithdrawDefault("Animations.Open.Sounds.ItemEvery", 1);
            getWithdrawSettings().set("ConfigVersion", 1.6D);
            changed = true;
        }

        if (version < 1.7D) {
            changed = migrateWithdrawIntIfUnchanged("Animations.Open.StartDelayTicks", 0, 1) || changed;
            changed = migrateWithdrawIntIfUnchanged("Animations.Open.ItemsPerTick", 1, 3) || changed;
            changed = migrateWithdrawStringIfUnchanged("Animations.Open.Sounds.Finish", "ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_PLAYER_LEVELUP") || changed;
            changed = migrateWithdrawDoubleIfUnchanged("Animations.Open.Sounds.StartVolume", 0.6D, 0.5D) || changed;
            changed = migrateWithdrawDoubleIfUnchanged("Animations.Open.Sounds.StartPitch", 0.8D, 1.0D) || changed;
            changed = migrateWithdrawDoubleIfUnchanged("Animations.Open.Sounds.ItemVolume", 0.45D, 0.35D) || changed;
            changed = migrateWithdrawDoubleIfUnchanged("Animations.Open.Sounds.ItemPitch", 1.25D, 1.2D) || changed;
            changed = migrateWithdrawDoubleIfUnchanged("Animations.Open.Sounds.RowVolume", 0.5D, 0.4D) || changed;
            changed = migrateWithdrawDoubleIfUnchanged("Animations.Open.Sounds.RowPitch", 1.45D, 1.35D) || changed;
            changed = migrateWithdrawDoubleIfUnchanged("Animations.Open.Sounds.FinishVolume", 0.7D, 0.45D) || changed;
            changed = migrateWithdrawDoubleIfUnchanged("Animations.Open.Sounds.FinishPitch", 1.0D, 1.6D) || changed;
            changed = migrateWithdrawIntIfUnchanged("Animations.Open.Sounds.ItemEvery", 1, 2) || changed;
            setWithdrawDefault("Animations.Open.PlaceholderItem.Enabled", true);
            setWithdrawDefault("Animations.Open.PlaceholderItem.Material", "BLACK_STAINED_GLASS_PANE");
            setWithdrawDefault("Animations.Open.PlaceholderItem.DisplayName", " ");
            setWithdrawDefault("Animations.Open.PlaceholderItem.DisplayLore", new ArrayList<>());
            setWithdrawDefault("Animations.Open.PlaceholderItem.Flags", Arrays.asList("HIDE_ATTRIBUTES", "HIDE_POTION_EFFECTS"));
            getWithdrawSettings().set("ConfigVersion", 1.7D);
            changed = true;
        }

        if (version < 1.8D) {
            if (!getWithdrawSettings().contains("Animations.Open.AnimateEmptySlots")) {
                getWithdrawSettings().set("Animations.Open.AnimateEmptySlots", true);
                changed = true;
            } else {
                changed = migrateWithdrawBooleanIfUnchanged("Animations.Open.AnimateEmptySlots", false, true) || changed;
            }
            getWithdrawSettings().set("ConfigVersion", 1.8D);
            changed = true;
        }

        if (version < 1.9D) {
            getWithdrawSettings().set("ConfigVersion", 1.9D);
            changed = true;
        }

        if (version < 2.0D) {
            getWithdrawSettings().set("ConfigVersion", 2.0D);
            changed = true;
        }

        if (version < 2.1D) {
            getWithdrawSettings().set("ConfigVersion", 2.1D);
            changed = true;
        }

        if (changed) {
            withdrawSettingsYml.saveConfig();
            getLogger().info("Migrated withdraw GUI layout defaults to ConfigVersion 2.1");
        }
    }

    private void setWithdrawDefault(String path, Object value) {
        if (!getWithdrawSettings().contains(path)) {
            getWithdrawSettings().set(path, value);
        }
    }

    private boolean removeWithdrawSettingsPath(String path) {
        if (!getWithdrawSettings().contains(path)) {
            return false;
        }

        getWithdrawSettings().remove(path);
        return true;
    }

    private boolean migrateWithdrawSlotIfUnchanged(String path, int oldSlot, int newSlot) {
        if (getWithdrawSettings().getInt(path, -1) != oldSlot) {
            return false;
        }

        getWithdrawSettings().set(path, newSlot);
        return true;
    }

    private boolean migrateWithdrawStringIfUnchanged(String path, String oldValue, String newValue) {
        String currentValue = getWithdrawSettings().getString(path, "");
        if (!oldValue.equalsIgnoreCase(currentValue)) {
            return false;
        }

        getWithdrawSettings().set(path, newValue);
        return true;
    }

    private boolean migrateWithdrawBooleanIfUnchanged(String path, boolean oldValue, boolean newValue) {
        if (!getWithdrawSettings().contains(path) || getWithdrawSettings().getBoolean(path) != oldValue) {
            return false;
        }

        getWithdrawSettings().set(path, newValue);
        return true;
    }

    private boolean migrateWithdrawIntIfUnchanged(String path, int oldValue, int newValue) {
        if (getWithdrawSettings().getInt(path, Integer.MIN_VALUE) != oldValue) {
            return false;
        }

        getWithdrawSettings().set(path, newValue);
        return true;
    }

    private boolean migrateWithdrawDoubleIfUnchanged(String path, double oldValue, double newValue) {
        double currentValue = getWithdrawSettings().getDouble(path, Double.NaN);
        if (Double.isNaN(currentValue) || Math.abs(currentValue - oldValue) > 0.0001D) {
            return false;
        }

        getWithdrawSettings().set(path, newValue);
        return true;
    }

    private boolean intListEquals(List<Integer> actual, int... expected) {
        if (actual == null || actual.size() != expected.length) {
            return false;
        }

        for (int i = 0; i < expected.length; i++) {
            Integer value = actual.get(i);
            if (value == null || value.intValue() != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private double getWithdrawSettingsVersion() {
        try {
            return Double.parseDouble(getWithdrawSettings().getString("ConfigVersion", "0"));
        } catch (Exception ignored) {
            return 0D;
        }
    }

    private void migrateDepositGuiDefaults() {
        double version = getDepositSettingsVersion();
        boolean changed = false;

        if (getDepositSettings().contains("Command.Permission")) {
            getDepositSettings().remove("Command.Permission");
            changed = true;
        }

        if (version >= 1.4D) {
            if (changed) {
                depositSettingsYml.saveConfig();
                getLogger().info("Removed obsolete depositor command permission config.");
            }
            return;
        }

        if (version < 1.1D) {
            changed = migrateDepositorDefaultButtonSlotsIfOldDefaults() || changed;
            getDepositSettings().set("ConfigVersion", 1.1D);
            changed = true;
        }

        if (version < 1.2D) {
            setDepositDefault("Sounds.Enabled", true);
            setDepositDefault("Sounds.Click", "UI_BUTTON_CLICK");
            setDepositDefault("Sounds.MenuOpen", "ENTITY_VILLAGER_TRADE");
            setDepositDefault("Sounds.Volume", 1D);
            setDepositDefault("Sounds.Pitch", 1D);

            setDepositDefault("Animations.Open.Enabled", false);
            setDepositDefault("Animations.Open.Type", "CENTER_OUT");
            setDepositDefault("Animations.Open.StartDelayTicks", 1);
            setDepositDefault("Animations.Open.TickDelay", 1);
            setDepositDefault("Animations.Open.ItemsPerTick", 3);
            setDepositDefault("Animations.Open.RowPauseTicks", 1);
            setDepositDefault("Animations.Open.AnimateEmptySlots", true);
            setDepositDefault("Animations.Open.LockClicksUntilFinished", true);
            setDepositDefault("Animations.Open.PlaceholderItem.Enabled", true);
            setDepositDefault("Animations.Open.PlaceholderItem.Material", "BLACK_STAINED_GLASS_PANE");
            setDepositDefault("Animations.Open.PlaceholderItem.DisplayName", " ");
            setDepositDefault("Animations.Open.PlaceholderItem.DisplayLore", new ArrayList<>());
            setDepositDefault("Animations.Open.PlaceholderItem.Flags", Arrays.asList("HIDE_ATTRIBUTES", "HIDE_POTION_EFFECTS"));
            setDepositDefault("Animations.Open.Sounds.Enabled", true);
            setDepositDefault("Animations.Open.Sounds.Start", "");
            setDepositDefault("Animations.Open.Sounds.Item", "UI_BUTTON_CLICK");
            setDepositDefault("Animations.Open.Sounds.Row", "");
            setDepositDefault("Animations.Open.Sounds.Finish", "ENTITY_PLAYER_LEVELUP");
            setDepositDefault("Animations.Open.Sounds.StartVolume", 0.5D);
            setDepositDefault("Animations.Open.Sounds.StartPitch", 1.0D);
            setDepositDefault("Animations.Open.Sounds.ItemVolume", 0.35D);
            setDepositDefault("Animations.Open.Sounds.ItemPitch", 1.2D);
            setDepositDefault("Animations.Open.Sounds.ItemPitchStep", 0.03D);
            setDepositDefault("Animations.Open.Sounds.ItemPitchMax", 2.0D);
            setDepositDefault("Animations.Open.Sounds.RowVolume", 0.4D);
            setDepositDefault("Animations.Open.Sounds.RowPitch", 1.35D);
            setDepositDefault("Animations.Open.Sounds.FinishVolume", 0.45D);
            setDepositDefault("Animations.Open.Sounds.FinishPitch", 1.6D);
            setDepositDefault("Animations.Open.Sounds.ItemEvery", 2);
            getDepositSettings().set("ConfigVersion", 1.2D);
            changed = true;
        }

        if (version < 1.3D) {
            if (intListEquals(getDepositSettings().getIntList("GUI.DepositSlots"),
                    11, 12, 13, 14, 15, 16,
                    20, 21, 22, 23, 24, 25,
                    29, 30, 31, 32, 33, 34)) {
                getDepositSettings().set("GUI.DepositSlots", Arrays.asList(
                        11, 12, 13, 14, 15, 16, 17,
                        20, 21, 22, 23, 24, 25, 26,
                        29, 30, 31, 32, 33, 34, 35
                ));
                changed = true;
            }
            getDepositSettings().set("ConfigVersion", 1.3D);
            changed = true;
        }

        if (version < 1.4D) {
            getDepositSettings().set("ConfigVersion", 1.4D);
            changed = true;
        }

        if (changed) {
            depositSettingsYml.saveConfig();
            getLogger().info("Migrated depositor GUI defaults to ConfigVersion 1.4");
        }
    }

    private void setDepositDefault(String path, Object value) {
        if (!getDepositSettings().contains(path)) {
            getDepositSettings().set(path, value);
        }
    }

    private boolean migrateDepositorDefaultButtonSlotsIfOldDefaults() {
        int depositSlot = getDepositSettings().getInt("GUI.Items.DepositButton.Slot", -1);
        int closeSlot = getDepositSettings().getInt("GUI.Items.CloseButton.Slot", -1);
        if (depositSlot != 50 || closeSlot != 49) {
            return false;
        }

        getDepositSettings().set("GUI.Items.DepositButton.Slot", 5);
        getDepositSettings().set("GUI.Items.CloseButton.Slot", 50);
        getDepositSettings().set("GUI.Items.CloseButton.DisplayName", "&c&lBack");
        return true;
    }

    private double getDepositSettingsVersion() {
        try {
            return Double.parseDouble(getDepositSettings().getString("ConfigVersion", "0"));
        } catch (Exception ignored) {
            return 0D;
        }
    }

    private void migrateLegacyDepositorSettings() {
        if (!depositSettingsYml.isNewFile() || !getSettings().isSection("Settings.Depositor")) {
            return;
        }

        copySectionToRoot(getSettings().getSection("Settings.Depositor"), "");
        migrateDepositorDefaultButtonSlotsIfOldDefaults();
        getDepositSettings().set("ConfigVersion", 1.1D);
        depositSettingsYml.saveConfig();
        getLogger().info("Migrated Settings.Depositor from config.yml to deposit-settings.yml");
    }

    private void copySectionToRoot(Section source, String targetPath) {
        if (source == null) {
            return;
        }

        for (Object rawKey : source.getKeys()) {
            String key = String.valueOf(rawKey);
            String childTarget = targetPath.isEmpty() ? key : targetPath + "." + key;
            Object value = source.get(key);
            if (value instanceof Section) {
                copySectionToRoot((Section) value, childTarget);
            } else {
                getDepositSettings().set(childTarget, value);
            }
        }
    }

    public void registerCommands() {
        if (beastLibCompatibilityGuard != null) {
            beastLibCompatibilityGuard.shutdown();
        }
        unregisterAdminCommand();
        unregisterDepositCommand();
        //purgeKnownAdminCommands();

        beastWithdrawCMD = new BeastWithdrawCMD(this,"beastwithdraw","Main admin command for BeastWithdraw plugin","/BeastWithdraw help",getSettings().getStringList("Settings.Aliases"));
        depositGuiManager = new DepositGuiManager(this);
        for (DepositGuiProfile profile : depositGuiManager.getCommandProfiles()) {
            depositGuiCommands.add(new DepositGuiCommand(this, depositGuiManager, profile));
        }
        withdrawGuiManager = new WithdrawGuiManager(this);
        for (WithdrawGuiProfile profile : withdrawGuiManager.getCommandProfiles()) {
            withdrawGuiCommands.add(new WithdrawGuiCommand(this, withdrawGuiManager, profile));
        }


        if (MinecraftVersion.getVersion().equals(MinecraftVersion.MC1_8_R3)) {
            LegacyCommandUtils.applyLegacyCommandFix(this,beastWithdrawCMD);
            for (DepositGuiCommand depositGuiCommand : depositGuiCommands) {
                LegacyCommandUtils.applyLegacyCommandFix(this, depositGuiCommand);
            }
            for (WithdrawGuiCommand withdrawGuiCommand : withdrawGuiCommands) {
                LegacyCommandUtils.applyLegacyCommandFix(this, withdrawGuiCommand);
            }
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
        shutdownUpdateManager();
        HandlerList.unregisterAll(this);
        if (beastLibCompatibilityGuard != null) {
            beastLibCompatibilityGuard.shutdown();
        }
        unregisterAdminCommand();
        unregisterDepositCommand();
        unregisterWithdrawGuiCommand();
        if (withdrawManager != null) {
            withdrawManager.unregisterAssetHandlers();
        }
        if (withdrawLogger != null) {
            withdrawLogger.shutdown();
        }
        if(metrics != null) metrics.shutdown();
    }

    private void shutdownUpdateManager() {
        if (updateManager != null) {
            updateManager.shutdown();
            updateManager = null;
        }
    }

    private void unregisterAdminCommand() {
        if (beastWithdrawCMD == null) {
            return;
        }

        beastWithdrawCMD.unregister();
        beastWithdrawCMD = null;
    }

    private void unregisterDepositCommand() {
        for (DepositGuiCommand depositGuiCommand : depositGuiCommands) {
            depositGuiCommand.unRegisterBukkitCommand();
        }
        depositGuiCommands.clear();

        depositGuiManager = null;
    }

    private void unregisterWithdrawGuiCommand() {
        for (WithdrawGuiCommand withdrawGuiCommand : withdrawGuiCommands) {
            withdrawGuiCommand.unRegisterBukkitCommand();
        }
        withdrawGuiCommands.clear();

        withdrawGuiManager = null;
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

    public YamlDocument getDepositSettings() {
        return depositSettingsYml.getConfig();
    }

    public YamlDocument getWithdrawSettings() {
        return withdrawSettingsYml.getConfig();
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

    public DepositGuiManager getDepositGuiManager() {
        return depositGuiManager;
    }

    public WithdrawGuiManager getWithdrawGuiManager() {
        return withdrawGuiManager;
    }

    public boolean isPremiumBuild() {
        String spigotUserId = SPIGOT_USER_ID == null ? "" : SPIGOT_USER_ID.trim();
        return !spigotUserId.isEmpty() && !getSpigotUserPlaceholder().equals(spigotUserId);
    }

    public String getSpigotUserId() {
        return SPIGOT_USER_ID;
    }

    private String getSpigotUserPlaceholder() {
        return new String(new char[]{
                '%', '%', '_', '_',
                'U', 'S', 'E', 'R',
                '_', '_', '%', '%'
        });
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
