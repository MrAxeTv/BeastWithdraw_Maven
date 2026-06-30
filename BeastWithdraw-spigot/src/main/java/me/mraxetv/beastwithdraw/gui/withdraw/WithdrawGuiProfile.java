package me.mraxetv.beastwithdraw.gui.withdraw;

import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WithdrawGuiProfile {
    private static final String TYPE_BASE = "WithdrawGUI";
    private static final String ACCEPTED_TYPES_BASE = "AcceptedTypes";

    private final BeastWithdrawPlugin plugin;
    private final String id;
    private final YamlDocument config;
    private final YamlDocument fallback;
    private final AssetHandler assetHandler;
    private final boolean mainProfile;
    private final boolean acceptAll;
    private final Set<AssetHandler> acceptedHandlers;

    public static WithdrawGuiProfile main(BeastWithdrawPlugin plugin) {
        return new WithdrawGuiProfile(plugin, "main", plugin.getWithdrawSettings(), null, null, true);
    }

    public static WithdrawGuiProfile asset(BeastWithdrawPlugin plugin, AssetHandler assetHandler) {
        return new WithdrawGuiProfile(
                plugin,
                assetHandler.getID().toLowerCase(Locale.ENGLISH),
                assetHandler.getConfig(),
                plugin.getWithdrawSettings(),
                assetHandler,
                false
        );
    }

    private WithdrawGuiProfile(BeastWithdrawPlugin plugin, String id, YamlDocument config, YamlDocument fallback,
                               AssetHandler assetHandler, boolean mainProfile) {
        this.plugin = plugin;
        this.id = id;
        this.config = config;
        this.fallback = fallback;
        this.assetHandler = assetHandler;
        this.mainProfile = mainProfile;
        this.acceptAll = resolveAcceptAll();
        this.acceptedHandlers = resolveAcceptedHandlers();
    }

    public String getId() {
        return id;
    }

    public boolean isMainProfile() {
        return mainProfile;
    }

    public AssetHandler getAssetHandler() {
        return assetHandler;
    }

    public boolean isEnabled() {
        if (mainProfile) {
            return getBoolean("Enabled", true);
        }
        return config.getBoolean(TYPE_BASE + ".Enabled", false);
    }

    public boolean isCommandEnabled() {
        if (mainProfile) {
            return isEnabled();
        }
        return getBoolean("Command.Enabled", false);
    }

    public String getCommandName() {
        String fallbackName = mainProfile ? "withdrawmenu" : assetHandler.getCommandName() + "gui";
        String command = commandString("Command.Name", fallbackName);
        return sanitizeCommand(command, fallbackName);
    }

    public List<String> getAliases() {
        Set<String> aliases = new LinkedHashSet<>();
        String command = getCommandName();
        for (String alias : commandStringList("Command.Aliases")) {
            String normalized = sanitizeCommand(alias, "");
            if (!normalized.isEmpty() && !normalized.equals(command)) {
                aliases.add(normalized);
            }
        }
        return new ArrayList<>(aliases);
    }

    public String getPermission() {
        if (mainProfile || assetHandler == null) {
            return "BeastWithdraw.WithdrawGui.Use";
        }
        return "BeastWithdraw." + assetHandler.getID() + ".WithdrawGui.Use";
    }

    public boolean accepts(AssetHandler handler) {
        return handler != null && (acceptAll || acceptedHandlers.contains(handler));
    }

    public Set<AssetHandler> getAcceptedHandlers() {
        return Collections.unmodifiableSet(acceptedHandlers);
    }

    public String applyPlaceholders(Player player, String value) {
        if (value == null) {
            return "";
        }

        String text = value;
        if (assetHandler != null) {
            text = assetHandler.applyPlaceholders(text, player);
            text = text.replace("%type%", assetHandler.getConfigName());
            text = text.replace("%type_id%", assetHandler.getID());
            text = text.replace("%withdraw_command%", assetHandler.getCommandName());
            if (player != null) {
                text = text.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalanceAsDouble(player)));
            }
        }
        return plugin.getUtils().setPlaceholders(player, text);
    }

    public boolean contains(String path) {
        return config.contains(localPath(path)) || (fallback != null && fallback.contains(path));
    }

    public boolean isSection(String path) {
        return config.isSection(localPath(path)) || (fallback != null && fallback.isSection(path));
    }

    public Section getSection(String path) {
        if (config.isSection(localPath(path))) {
            return config.getSection(localPath(path));
        }
        return fallback == null ? null : fallback.getSection(path);
    }

    public String getString(String path, String defaultValue) {
        if (config.contains(localPath(path))) {
            return config.getString(localPath(path), defaultValue);
        }
        return fallback == null ? defaultValue : fallback.getString(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        if (config.contains(localPath(path))) {
            return config.getInt(localPath(path), defaultValue);
        }
        return fallback == null ? defaultValue : fallback.getInt(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        if (config.contains(localPath(path))) {
            return config.getDouble(localPath(path), defaultValue);
        }
        return fallback == null ? defaultValue : fallback.getDouble(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        if (config.contains(localPath(path))) {
            return config.getBoolean(localPath(path), defaultValue);
        }
        return fallback == null ? defaultValue : fallback.getBoolean(path, defaultValue);
    }

    public List<String> getStringList(String path) {
        if (config.contains(localPath(path))) {
            return config.getStringList(localPath(path));
        }
        return fallback == null ? Collections.emptyList() : fallback.getStringList(path);
    }

    public List<Integer> getIntList(String path) {
        if (config.contains(localPath(path))) {
            return config.getIntList(localPath(path));
        }
        return fallback == null ? Collections.emptyList() : fallback.getIntList(path);
    }

    private boolean resolveAcceptAll() {
        if (!mainProfile) {
            return false;
        }

        String mode = getString(ACCEPTED_TYPES_BASE + ".Mode", "ALL");
        if (mode == null) {
            return true;
        }

        String normalized = mode.trim().toUpperCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
        return !("SPECIFIC".equals(normalized) || "LIST".equals(normalized) || "TYPES".equals(normalized));
    }

    private Set<AssetHandler> resolveAcceptedHandlers() {
        Set<AssetHandler> handlers = new LinkedHashSet<>();
        if (!mainProfile) {
            handlers.add(assetHandler);
            return handlers;
        }

        if (acceptAll) {
            return handlers;
        }

        for (String type : getStringList(ACCEPTED_TYPES_BASE + ".Types")) {
            AssetHandler handler = plugin.getWithdrawManager().getAssetHandler(type);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    private String commandString(String path, String defaultValue) {
        String value;
        if (config.contains(localPath(path))) {
            value = config.getString(localPath(path), defaultValue);
        } else {
            value = fallback == null ? defaultValue : fallback.getString(path, defaultValue);
        }
        return resolveCommandPlaceholders(value);
    }

    private List<String> commandStringList(String path) {
        List<String> values;
        if (config.contains(localPath(path))) {
            values = config.getStringList(localPath(path));
        } else {
            values = fallback == null ? Collections.emptyList() : fallback.getStringList(path);
        }

        List<String> resolved = new ArrayList<>();
        for (String value : values) {
            resolved.add(resolveCommandPlaceholders(value));
        }
        return resolved;
    }

    private String localPath(String path) {
        return mainProfile ? path : TYPE_BASE + "." + path;
    }

    private String resolveCommandPlaceholders(String value) {
        if (value == null) {
            return null;
        }
        if (assetHandler == null) {
            return value;
        }
        return assetHandler.applyPlaceholders(value, null)
                .replace("%type%", assetHandler.getConfigName())
                .replace("%type_id%", assetHandler.getID())
                .replace("%withdraw_command%", assetHandler.getCommandName());
    }

    private String sanitizeCommand(String value, String fallbackValue) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
        normalized = normalized.replaceAll("[^a-z0-9_-]", "");
        if (!normalized.isEmpty()) {
            return normalized;
        }

        String fallbackCommand = fallbackValue == null ? "" : fallbackValue.trim().toLowerCase(Locale.ENGLISH);
        fallbackCommand = fallbackCommand.replaceAll("[^a-z0-9_-]", "");
        return fallbackCommand.isEmpty() ? "withdrawmenu" : fallbackCommand;
    }
}
