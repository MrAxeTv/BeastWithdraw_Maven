package me.mraxetv.beastwithdraw.gui.depositor;

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

public final class DepositGuiProfile {
    private static final String ACCEPTED_TYPES_BASE = "AcceptedTypes";

    private final BeastWithdrawPlugin plugin;
    private final String id;
    private final YamlDocument config;
    private final YamlDocument fallback;
    private final AssetHandler assetHandler;
    private final boolean mainProfile;
    private final boolean acceptAll;
    private final Set<AssetHandler> acceptedHandlers;

    public static DepositGuiProfile main(BeastWithdrawPlugin plugin) {
        return new DepositGuiProfile(plugin, "main", plugin.getDepositSettings(), null, null, true);
    }

    public static DepositGuiProfile asset(BeastWithdrawPlugin plugin, AssetHandler assetHandler) {
        return new DepositGuiProfile(
                plugin,
                assetHandler.getID().toLowerCase(Locale.ENGLISH),
                assetHandler.getDepositConfig(),
                plugin.getDepositSettings(),
                assetHandler,
                false
        );
    }

    private DepositGuiProfile(BeastWithdrawPlugin plugin, String id, YamlDocument config, YamlDocument fallback,
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
        return config.getBoolean("Enabled", false);
    }

    public String getCommandName() {
        String fallbackName = mainProfile ? "depositnotes" : assetHandler.getCommandName() + "deposit";
        String command = commandString("Command.Name", fallbackName);
        return sanitizeCommand(command, fallbackName);
    }

    public List<String> getAliases() {
        Set<String> aliases = new LinkedHashSet<>();
        String command = getCommandName();
        for (String alias : commandStringList("Command.Aliases")) {
            if (alias == null || alias.trim().isEmpty()) {
                continue;
            }

            String normalized = sanitizeCommand(alias, "");
            if (!normalized.isEmpty() && !normalized.equals(command)) {
                aliases.add(normalized);
            }
        }
        return new ArrayList<>(aliases);
    }

    public String getPermission() {
        if (mainProfile || assetHandler == null) {
            return "BeastWithdraw.Depositor.Use";
        }
        if ("McMMORedeemSkillCredits".equalsIgnoreCase(assetHandler.getID())) {
            return "BeastWithdraw.McMMORedeemCredits.Depositor.Skill.Use";
        }
        return "BeastWithdraw." + assetHandler.getID() + ".Depositor.Use";
    }

    public boolean accepts(AssetHandler handler) {
        return handler != null && (acceptAll || acceptedHandlers.contains(handler));
    }

    public Set<AssetHandler> getAcceptedHandlers() {
        return Collections.unmodifiableSet(acceptedHandlers);
    }

    public boolean acceptsAllTypes() {
        return acceptAll;
    }

    public String applyPlaceholders(Player player, String value) {
        if (value == null) {
            return "";
        }

        String text = value;
        if (assetHandler != null) {
            text = assetHandler.applyPlaceholders(text, player);
        }
        return plugin.getUtils().setPlaceholders(player, text);
    }

    public boolean contains(String path) {
        return config.contains(path) || (fallback != null && fallback.contains(path));
    }

    public boolean isSection(String path) {
        return config.isSection(path) || (fallback != null && fallback.isSection(path));
    }

    public Section getSection(String path) {
        if (config.isSection(path)) {
            return config.getSection(path);
        }
        return fallback == null ? null : fallback.getSection(path);
    }

    public String getString(String path, String defaultValue) {
        if (config.contains(path)) {
            return config.getString(path, defaultValue);
        }
        return fallback == null ? defaultValue : fallback.getString(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        if (config.contains(path)) {
            return config.getInt(path, defaultValue);
        }
        return fallback == null ? defaultValue : fallback.getInt(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        if (config.contains(path)) {
            return config.getDouble(path, defaultValue);
        }
        return fallback == null ? defaultValue : fallback.getDouble(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        if (config.contains(path)) {
            return config.getBoolean(path, defaultValue);
        }
        return fallback == null ? defaultValue : fallback.getBoolean(path, defaultValue);
    }

    public List<String> getStringList(String path) {
        if (config.contains(path)) {
            return config.getStringList(path);
        }
        return fallback == null ? Collections.emptyList() : fallback.getStringList(path);
    }

    public List<Integer> getIntList(String path) {
        if (config.contains(path)) {
            return config.getIntList(path);
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
        if (config.contains(path)) {
            value = config.getString(path, defaultValue);
        } else if (mainProfile && fallback != null && fallback.contains(path)) {
            value = fallback.getString(path, defaultValue);
        } else {
            value = defaultValue;
        }

        return resolveCommandPlaceholders(value);
    }

    private List<String> commandStringList(String path) {
        List<String> values;
        if (config.contains(path)) {
            values = config.getStringList(path);
        } else if (mainProfile && fallback != null && fallback.contains(path)) {
            values = fallback.getStringList(path);
        } else {
            values = Collections.emptyList();
        }

        List<String> resolved = new ArrayList<>();
        for (String value : values) {
            resolved.add(resolveCommandPlaceholders(value));
        }
        return resolved;
    }

    private String resolveCommandPlaceholders(String value) {
        if (value == null) {
            return null;
        }

        if (assetHandler != null) {
            return assetHandler.applyPlaceholders(value, null);
        }
        return value;
    }

    private String sanitizeCommand(String value, String fallbackValue) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
        normalized = normalized.replaceAll("[^a-z0-9_-]", "");
        if (!normalized.isEmpty()) {
            return normalized;
        }

        String fallbackCommand = fallbackValue == null ? "" : fallbackValue.trim().toLowerCase(Locale.ENGLISH);
        fallbackCommand = fallbackCommand.replaceAll("[^a-z0-9_-]", "");
        return fallbackCommand.isEmpty() ? "depositnotes" : fallbackCommand;
    }
}
