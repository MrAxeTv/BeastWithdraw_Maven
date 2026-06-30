package me.mraxetv.beastwithdraw.compatibility;

import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.BeastLibUpdateRequiredCMD;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BeastLibCompatibilityGuard {

    private static final String REQUIRED_BEASTLIB_VERSION = "1.6.22";
    private static final String BEASTLIB_UPDATE_LINK = "https://modrinth.com/plugin/beastlib";

    private final BeastWithdrawPlugin pl;
    private final List<ShortCommand> fallbackCommands = new ArrayList<>();

    public BeastLibCompatibilityGuard(BeastWithdrawPlugin pl) {
        this.pl = pl;
    }

    public boolean isCompatible() {
        Plugin beastLib = pl.getServer().getPluginManager().getPlugin("BeastLib");
        if (beastLib == null || !beastLib.isEnabled()) {
            return false;
        }

        return compareVersions(beastLib.getDescription().getVersion(), REQUIRED_BEASTLIB_VERSION) >= 0;
    }

    public void enableFallback() {
        registerFallbackCommands();
        pl.getServer().getScheduler().runTaskLater(pl, () -> sendWarning(pl.getServer().getConsoleSender()), 1L);
    }

    public void shutdown() {
        for (ShortCommand command : fallbackCommands) {
            if (command != null) {
                command.unRegisterBukkitCommand();
            }
        }
        fallbackCommands.clear();
    }

    public void sendWarning(CommandSender sender) {
        String detectedVersion = getDetectedBeastLibVersion();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7[&4Beast&bWithdraw&7] &cBeastWithdraw is not working because BeastLib "
                        + REQUIRED_BEASTLIB_VERSION + " or higher is required."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7[&4Beast&bWithdraw&7] &cDetected BeastLib version: &e" + detectedVersion));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7[&4Beast&bWithdraw&7] &cUpdate BeastLib: &b" + BEASTLIB_UPDATE_LINK));
    }

    private String getDetectedBeastLibVersion() {
        Plugin beastLib = pl.getServer().getPluginManager().getPlugin("BeastLib");
        if (beastLib == null) {
            return "not installed";
        }
        if (!beastLib.isEnabled()) {
            return beastLib.getDescription().getVersion() + " (disabled)";
        }
        return beastLib.getDescription().getVersion();
    }

    private void registerFallbackCommands() {
        shutdown();

        YamlConfiguration config = loadFallbackYaml("config.yml");
        registerFallbackCommand(
                "beastwithdraw",
                getFallbackStringList(config, "Settings.Aliases", Arrays.asList("bw", "bwa"))
        );

        YamlConfiguration withdrawSettings = loadFallbackYaml("withdraw-settings.yml");
        if (withdrawSettings.getBoolean("Enabled", true)) {
            registerFallbackCommand(
                    withdrawSettings.getString("Command.Name", "withdrawmenu"),
                    getFallbackStringList(withdrawSettings, "Command.Aliases", Arrays.asList("withdrawgui", "notewithdraw", "withdrawnotes"))
            );
        }

        registerWithdrawFallbackCommand(config, "XpBottle", "XpBottle", "Withdraws/XpBottle/Withdraw.yml", "Withdraws/XpBottle/XpBottle.yml", "Withdraws/XpBottle.yml", true);
        registerWithdrawFallbackCommand(config, "CashNote", "CashNote", "Withdraws/CashNote/Withdraw.yml", "Withdraws/CashNote/CashNote.yml", "Withdraws/CashNote.yml", true);
        registerWithdrawFallbackCommand(config, "PlayerPointsNote", "PlayerPointsNote", "Withdraws/PlayerPointsNote/Withdraw.yml", "Withdraws/PlayerPointsNote/PlayerPointsNote.yml", "Withdraws/PlayerPointsNote.yml", false);
        registerWithdrawFallbackCommand(config, "BeastLifeSteal", "heartwithdraw", "Withdraws/BeastLifeSteal/Withdraw.yml", "Withdraws/BeastLifeSteal/BeastLifeSteal.yml", "Withdraws/BeastLifeSteal.yml", false);

        pl.getServer().getScheduler().runTaskLater(pl, this::syncServerCommands, 1L);
    }

    private void registerWithdrawFallbackCommand(YamlConfiguration config, String configKey, String commandName, String resourcePath, String legacyDiskPath, boolean defaultEnabled) {
        registerWithdrawFallbackCommand(config, configKey, commandName, resourcePath, legacyDiskPath, null, defaultEnabled);
    }

    private void registerWithdrawFallbackCommand(YamlConfiguration config, String configKey, String commandName,
                                                 String resourcePath, String legacyDiskPath, String legacyFlatDiskPath,
                                                 boolean defaultEnabled) {
        if (!config.getBoolean("Settings.Withdraws." + configKey + ".Enabled", defaultEnabled)) {
            return;
        }

        YamlConfiguration withdrawConfig = loadFallbackYaml(resourcePath, legacyDiskPath, legacyFlatDiskPath);
        registerFallbackCommand(
                commandName,
                getFallbackStringList(withdrawConfig, "Settings.Aliases", Collections.emptyList())
        );
    }

    private void registerFallbackCommand(String name, List<String> aliases) {
        fallbackCommands.add(new BeastLibUpdateRequiredCMD(pl, this, name, sanitizeAliases(name, aliases)));
    }

    private YamlConfiguration loadFallbackYaml(String relativePath) {
        return loadFallbackYaml(relativePath, null, null);
    }

    private YamlConfiguration loadFallbackYaml(String relativePath, String legacyDiskPath) {
        return loadFallbackYaml(relativePath, legacyDiskPath, null);
    }

    private YamlConfiguration loadFallbackYaml(String relativePath, String legacyDiskPath, String legacyFlatDiskPath) {
        File diskFile = new File(pl.getDataFolder(), relativePath);
        if (diskFile.exists()) {
            return YamlConfiguration.loadConfiguration(diskFile);
        }

        if (legacyDiskPath != null) {
            File legacyDiskFile = new File(pl.getDataFolder(), legacyDiskPath);
            if (legacyDiskFile.exists()) {
                return YamlConfiguration.loadConfiguration(legacyDiskFile);
            }
        }

        if (legacyFlatDiskPath != null) {
            File legacyFlatDiskFile = new File(pl.getDataFolder(), legacyFlatDiskPath);
            if (legacyFlatDiskFile.exists()) {
                return YamlConfiguration.loadConfiguration(legacyFlatDiskFile);
            }
        }

        InputStream stream = pl.getResource(relativePath);
        if (stream == null) {
            return new YamlConfiguration();
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException ignored) {
            return new YamlConfiguration();
        }
    }

    private List<String> getFallbackStringList(YamlConfiguration config, String path, List<String> defaults) {
        List<String> values = config.getStringList(path);
        if (values == null || values.isEmpty()) {
            return defaults;
        }
        return values;
    }

    private List<String> sanitizeAliases(String commandName, List<String> aliases) {
        Set<String> uniqueAliases = new LinkedHashSet<>();
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias == null || alias.trim().isEmpty()) {
                    continue;
                }

                String normalized = alias.trim().toLowerCase();
                if (!normalized.equalsIgnoreCase(commandName)) {
                    uniqueAliases.add(normalized);
                }
            }
        }
        return new ArrayList<>(uniqueAliases);
    }

    private int compareVersions(String currentVersion, String requiredVersion) {
        List<Integer> current = parseVersion(currentVersion);
        List<Integer> required = parseVersion(requiredVersion);
        int length = Math.max(current.size(), required.size());

        for (int i = 0; i < length; i++) {
            int currentPart = i < current.size() ? current.get(i) : 0;
            int requiredPart = i < required.size() ? required.get(i) : 0;
            if (currentPart != requiredPart) {
                return Integer.compare(currentPart, requiredPart);
            }
        }

        return 0;
    }

    private List<Integer> parseVersion(String version) {
        List<Integer> parts = new ArrayList<>();
        if (version == null) {
            return parts;
        }

        for (String part : version.split("[^0-9]+")) {
            if (part.isEmpty()) {
                continue;
            }
            try {
                parts.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
            }
        }

        return parts;
    }

    private void syncServerCommands() {
        try {
            Bukkit.getServer().getClass().getMethod("syncCommands").invoke(Bukkit.getServer());
        } catch (Exception ignored) {
        }
    }
}
