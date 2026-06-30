package me.mraxetv.beastwithdraw.managers;

import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NoteItemSettings {
    private NoteItemSettings() {
    }

    public static NoteVisual resolve(YamlDocument config, double amount) {
        return resolve(config, amount, null);
    }

    public static NoteVisual resolve(YamlDocument config, double amount, String overrideId) {
        NoteVisual visual = readBase(config);
        if (config == null || !config.getBoolean("Settings.AmountOverrides.Enabled", false)) return visual;

        String forcedId = normalizeOverrideId(overrideId);
        if (forcedId != null) {
            for (Map<?, ?> entry : config.getMapList("Settings.AmountOverrides.Ranges")) {
                if (!forcedId.equalsIgnoreCase(getOverrideId(entry))) continue;
                return visual.with(readOverride(entry));
            }
        }

        for (Map<?, ?> entry : config.getMapList("Settings.AmountOverrides.Ranges")) {
            Double min = getDouble(entry, "min", "Min");
            if (min == null) continue;
            Double max = getDouble(entry, "max", "Max");
            if (amount >= min && (max == null || amount <= max)) {
                visual = visual.with(readOverride(entry));
            }
        }
        return visual;
    }

    public static List<String> getOverrideIds(YamlDocument config) {
        List<String> ids = new ArrayList<>();
        if (config == null || !config.getBoolean("Settings.AmountOverrides.Enabled", false)) return ids;

        for (Map<?, ?> entry : config.getMapList("Settings.AmountOverrides.Ranges")) {
            String id = getOverrideId(entry);
            if (id != null && !containsIgnoreCase(ids, id)) ids.add(id);
        }
        return ids;
    }

    public static boolean hasOverrideId(YamlDocument config, String overrideId) {
        String normalized = normalizeOverrideId(overrideId);
        if (normalized == null) return false;
        for (String id : getOverrideIds(config)) {
            if (id.equalsIgnoreCase(normalized)) return true;
        }
        return false;
    }

    public static String getCanonicalOverrideId(YamlDocument config, String overrideId) {
        String normalized = normalizeOverrideId(overrideId);
        if (normalized == null) return null;
        for (String id : getOverrideIds(config)) {
            if (id.equalsIgnoreCase(normalized)) return id;
        }
        return null;
    }

    private static NoteVisual readBase(YamlDocument config) {
        String defaultName = getString(config, null, "Settings.Name");
        if (defaultName == null) {
            defaultName = getString(config, "<gradient:#6dff8a:#ffd76a>Bank Note</gradient> <gray>(Right-Click)</gray>",
                    "Settings.Player.Name", "Settings.Server.Name");
        }

        List<String> defaultLore = getStringList(config, null, "Settings.Lore");
        if (defaultLore == null) {
            defaultLore = getStringList(config, Arrays.asList(
                    "<dark_gray>[</dark_gray><gradient:#6dff8a:#ffd76a>Value</gradient><dark_gray>]</dark_gray> <white>%amount%</white>",
                    "%tax%",
                    "<gray>Right-click to redeem this note.</gray>"
            ), "Settings.Player.Lore", "Settings.Server.Lore");
        }

        IntegerOverride customModelData = getIntegerOverride(config, "Settings.CustomModelData");
        IntegerOverride data = getIntegerOverride(config, "Settings.Data");

        return new NoteVisual(
                getString(config, "PAPER", "Settings.Item", "Settings.Material"),
                customModelData.set,
                customModelData.value,
                data.set,
                data.value,
                getGlowEnabled(config),
                getStringAllowEmpty(config, "GREEN", "Settings.Glow.Color"),
                getStringAllowEmpty(config, null, "Settings.SkullTexture", "Settings.SkullName"),
                getStringAllowEmpty(config, "", "Settings.CustomName"),
                getStringList(config, new ArrayList<String>(), "Settings.Flags"),
                getEnchantMap(config, "Settings.Enchants"),
                defaultName,
                defaultLore,
                getStringAllowEmpty(config, null, "Settings.Player.Name"),
                getStringList(config, null, "Settings.Player.Lore"),
                getStringAllowEmpty(config, null, "Settings.Server.Name"),
                getStringList(config, null, "Settings.Server.Lore"),
                readSound(config, "Settings.Sounds.Withdraw", true, "ENTITY_GENERIC_EXPLODE"),
                readSound(config, "Settings.Sounds.Redeem", true, "ENTITY_EXPERIENCE_ORB_PICKUP"));
    }

    private static NoteOverride readOverride(Map<?, ?> entry) {
        IntegerOverride customModelData = getIntegerOverride(entry, "CustomModelData", "customModelData", "ModelData", "modelData", "model", "Model");
        IntegerOverride data = getIntegerOverride(entry, "Data", "data");
        return new NoteOverride(
                getString(entry, "Item", "Material", "material"),
                customModelData == null ? null : customModelData.set,
                customModelData == null ? null : customModelData.value,
                data == null ? null : data.set,
                data == null ? null : data.value,
                getBoolean(entry, "Glow.Enabled", "glow.enabled", "Glow", "glow"),
                getStringAllowEmpty(entry, "Glow.Color", "glow.color", "GlowColor", "glowColor"),
                getStringAllowEmpty(entry, "SkullTexture", "SkullName", "skullTexture", "skullName"),
                getStringAllowEmpty(entry, "CustomName", "customName", "Hologram.Name", "hologram.name"),
                getStringList(entry, "Flags", "flags"),
                getEnchantMap(entry, "Enchants", "enchants"),
                getStringAllowEmpty(entry, "Name", "name"),
                getStringList(entry, "Lore", "lore"),
                getStringAllowEmpty(entry, "Player.Name", "player.name", "PlayerName", "playerName"),
                getStringList(entry, "Player.Lore", "player.lore", "PlayerLore", "playerLore"),
                getStringAllowEmpty(entry, "Server.Name", "server.name", "ServerName", "serverName"),
                getStringList(entry, "Server.Lore", "server.lore", "ServerLore", "serverLore"),
                readSoundOverride(entry, "Withdraw"),
                readSoundOverride(entry, "Redeem"));
    }

    private static String getOverrideId(Map<?, ?> entry) {
        return getString(entry, "Id", "ID", "id", "OverrideId", "overrideId", "OverrideID", "overrideID", "OverrideName", "overrideName");
    }

    private static boolean getGlowEnabled(YamlDocument config) {
        if (config == null) return true;
        if (config.contains("Settings.Glow.Enabled")) return config.getBoolean("Settings.Glow.Enabled", true);
        if (config.contains("Settings.Glow") && !config.isSection("Settings.Glow")) {
            return config.getBoolean("Settings.Glow", true);
        }
        return true;
    }

    private static SoundSettings readSound(YamlDocument config, String path, boolean enabledFallback, String soundFallback) {
        return new SoundSettings(
                config == null ? enabledFallback : config.getBoolean(path + ".Enabled", enabledFallback),
                getString(config, soundFallback, path + ".Sound"),
                config == null ? 1.0F : config.getDouble(path + ".Volume", 1.0).floatValue(),
                config == null ? 1.0F : config.getDouble(path + ".Pitch", 1.0).floatValue());
    }

    private static SoundOverride readSoundOverride(Map<?, ?> entry, String type) {
        String prefix = "Sounds." + type + ".";
        String lower = "sounds." + type.toLowerCase() + ".";
        Boolean enabled = getBoolean(entry, prefix + "Enabled", lower + "enabled", type + "Sound.Enabled", type.toLowerCase() + "Sound.enabled");
        String sound = getString(entry, prefix + "Sound", lower + "sound", type + "Sound.Sound", type.toLowerCase() + "Sound.sound");
        Float volume = getFloat(entry, prefix + "Volume", lower + "volume", type + "Sound.Volume", type.toLowerCase() + "Sound.volume");
        Float pitch = getFloat(entry, prefix + "Pitch", lower + "pitch", type + "Sound.Pitch", type.toLowerCase() + "Sound.pitch");
        if (enabled == null && sound == null && volume == null && pitch == null) return null;
        return new SoundOverride(enabled, sound, volume, pitch);
    }

    private static String getString(YamlDocument config, String fallback, String... paths) {
        if (config == null) return fallback;
        for (String path : paths) {
            if (!config.contains(path)) continue;
            String value = config.getString(path, null);
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return fallback;
    }

    private static String getStringAllowEmpty(YamlDocument config, String fallback, String... paths) {
        if (config == null) return fallback;
        for (String path : paths) {
            if (!config.contains(path)) continue;
            String value = config.getString(path, null);
            return value == null ? "" : value.trim();
        }
        return fallback;
    }

    private static List<String> getStringList(YamlDocument config, List<String> fallback, String... paths) {
        if (config == null) return copyList(fallback);
        for (String path : paths) {
            if (config.contains(path)) return config.getStringList(path);
        }
        return fallback == null ? null : copyList(fallback);
    }

    private static IntegerOverride getIntegerOverride(YamlDocument config, String path) {
        if (config == null || !config.contains(path)) return IntegerOverride.clear();
        String raw = config.getString(path, null);
        if (isClearValue(raw)) return IntegerOverride.clear();
        return IntegerOverride.set(config.getInt(path, 0));
    }

    private static Map<String, Integer> getEnchantMap(YamlDocument config, String path) {
        Map<String, Integer> enchants = new LinkedHashMap<>();
        if (config == null || !config.contains(path)) return enchants;
        Section section = config.getSection(path);
        if (section == null) return enchants;
        for (String key : section.getRoutesAsStrings(false)) {
            enchants.put(key, section.getInt(key, 1));
        }
        return enchants;
    }

    private static String getString(Map<?, ?> map, String... keys) {
        Object value = getValue(map, keys);
        if (value == null) return null;
        String string = String.valueOf(value).trim();
        return string.isEmpty() ? null : string;
    }

    private static String getStringAllowEmpty(Map<?, ?> map, String... keys) {
        ValueLookup lookup = findValue(map, keys);
        if (!lookup.found) return null;
        return lookup.value == null ? "" : String.valueOf(lookup.value).trim();
    }

    private static Boolean getBoolean(Map<?, ?> map, String... keys) {
        Object value = getValue(map, keys);
        if (value instanceof Map<?, ?> || value instanceof Section) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value == null) return null;
        String string = String.valueOf(value).trim();
        if (string.isEmpty()) return null;
        return Boolean.parseBoolean(string);
    }

    private static Float getFloat(Map<?, ?> map, String... keys) {
        Object value = getValue(map, keys);
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value == null) return null;
        try {
            return Float.parseFloat(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double getDouble(Map<?, ?> map, String... keys) {
        Object value = getValue(map, keys);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value == null) return null;
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static IntegerOverride getIntegerOverride(Map<?, ?> map, String... keys) {
        ValueLookup lookup = findValue(map, keys);
        if (!lookup.found) return null;
        if (isClearValue(lookup.value)) return IntegerOverride.clear();
        Integer value = toInteger(lookup.value);
        return value == null ? null : IntegerOverride.set(value);
    }

    private static List<String> getStringList(Map<?, ?> map, String... keys) {
        ValueLookup lookup = findValue(map, keys);
        if (!lookup.found) return null;
        if (isClearValue(lookup.value)) return new ArrayList<>();
        if (lookup.value instanceof List<?>) {
            List<String> values = new ArrayList<>();
            for (Object object : (List<?>) lookup.value) {
                if (object != null) values.add(String.valueOf(object));
            }
            return values;
        }
        return Arrays.asList(String.valueOf(lookup.value));
    }

    private static Map<String, Integer> getEnchantMap(Map<?, ?> map, String... keys) {
        ValueLookup lookup = findValue(map, keys);
        if (!lookup.found) return null;
        if (isClearValue(lookup.value)) return new LinkedHashMap<>();
        return readEnchantMap(lookup.value);
    }

    private static Map<String, Integer> readEnchantMap(Object value) {
        Map<String, Integer> enchants = new LinkedHashMap<>();
        if (value instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (entry.getKey() == null) continue;
                enchants.put(String.valueOf(entry.getKey()), toInteger(entry.getValue(), 1));
            }
        }
        if (value instanceof List<?>) {
            for (Object object : (List<?>) value) {
                if (object instanceof Map<?, ?>) {
                    Map<?, ?> map = (Map<?, ?>) object;
                    String enchant = getString(map, "Enchant", "enchant", "Name", "name", "Type", "type");
                    Integer level = toInteger(getValue(map, "Level", "level"));
                    if (enchant != null) enchants.put(enchant, level == null ? 1 : level);
                }
            }
        }
        return enchants;
    }

    private static Object getValue(Map<?, ?> map, String... keys) {
        ValueLookup lookup = findValue(map, keys);
        return lookup.found ? lookup.value : null;
    }

    private static ValueLookup findValue(Map<?, ?> map, String... keys) {
        if (map == null) return ValueLookup.missing();
        for (String key : keys) {
            ValueLookup direct = getDirectValue(map, key);
            if (direct.found) return direct;
            ValueLookup nested = getNestedValue(map, key);
            if (nested.found) return nested;
        }
        return ValueLookup.missing();
    }

    private static ValueLookup getDirectValue(Map<?, ?> map, String key) {
        if (map == null || key == null) return ValueLookup.missing();
        if (map.containsKey(key)) return ValueLookup.found(map.get(key));
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && key.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                return ValueLookup.found(entry.getValue());
            }
        }
        return ValueLookup.missing();
    }

    private static ValueLookup getNestedValue(Map<?, ?> map, String path) {
        if (map == null || path == null || !path.contains(".")) return ValueLookup.missing();
        Object current = map;
        for (String part : path.split("\\.")) {
            if (current instanceof Map<?, ?>) {
                ValueLookup lookup = getDirectValue((Map<?, ?>) current, part);
                if (!lookup.found) return ValueLookup.missing();
                current = lookup.value;
            } else {
                return ValueLookup.missing();
            }
        }
        return ValueLookup.found(current);
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value == null) return null;
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int toInteger(Object value, int fallback) {
        Integer integer = toInteger(value);
        return integer == null ? fallback : integer;
    }

    private static boolean isClearValue(Object value) {
        if (value == null) return true;
        String string = String.valueOf(value).trim();
        if (string.isEmpty()) return true;
        return string.equalsIgnoreCase("clear")
                || string.equalsIgnoreCase("none")
                || string.equalsIgnoreCase("null")
                || string.equalsIgnoreCase("remove")
                || string.equalsIgnoreCase("unset")
                || string.equalsIgnoreCase("disabled");
    }

    private static String normalizeOverrideId(String overrideId) {
        if (overrideId == null) return null;
        String normalized = overrideId.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        if (values == null || value == null) return false;
        for (String current : values) {
            if (value.equalsIgnoreCase(current)) return true;
        }
        return false;
    }

    private static List<String> copyList(List<String> values) {
        return values == null ? null : new ArrayList<>(values);
    }

    private static Map<String, Integer> copyMap(Map<String, Integer> values) {
        return values == null ? null : new LinkedHashMap<>(values);
    }

    public static final class NoteVisual {
        private final String item;
        private final boolean customModelDataSet;
        private final int customModelData;
        private final boolean dataSet;
        private final int data;
        private final boolean glowEnabled;
        private final String glowColor;
        private final String skullTexture;
        private final String customName;
        private final List<String> flags;
        private final Map<String, Integer> enchants;
        private final String name;
        private final List<String> lore;
        private final String playerName;
        private final List<String> playerLore;
        private final String serverName;
        private final List<String> serverLore;
        private final SoundSettings withdrawSound;
        private final SoundSettings redeemSound;

        private NoteVisual(
                String item,
                boolean customModelDataSet,
                int customModelData,
                boolean dataSet,
                int data,
                boolean glowEnabled,
                String glowColor,
                String skullTexture,
                String customName,
                List<String> flags,
                Map<String, Integer> enchants,
                String name,
                List<String> lore,
                String playerName,
                List<String> playerLore,
                String serverName,
                List<String> serverLore,
                SoundSettings withdrawSound,
                SoundSettings redeemSound) {
            this.item = item == null || item.trim().isEmpty() ? "PAPER" : item.trim();
            this.customModelDataSet = customModelDataSet;
            this.customModelData = customModelData;
            this.dataSet = dataSet;
            this.data = data;
            this.glowEnabled = glowEnabled;
            this.glowColor = glowColor;
            this.skullTexture = skullTexture;
            this.customName = customName;
            this.flags = copyList(flags);
            this.enchants = copyMap(enchants);
            this.name = name == null ? "" : name;
            this.lore = lore == null ? new ArrayList<String>() : copyList(lore);
            this.playerName = playerName;
            this.playerLore = copyList(playerLore);
            this.serverName = serverName;
            this.serverLore = copyList(serverLore);
            this.withdrawSound = withdrawSound == null ? SoundSettings.disabled("ENTITY_GENERIC_EXPLODE") : withdrawSound;
            this.redeemSound = redeemSound == null ? SoundSettings.disabled("ENTITY_EXPERIENCE_ORB_PICKUP") : redeemSound;
        }

        private NoteVisual with(NoteOverride override) {
            if (override == null) return this;
            String mergedName = override.name == null ? name : override.name;
            List<String> mergedLore = override.lore == null ? lore : override.lore;
            String mergedPlayerName = override.playerName != null ? override.playerName : (override.name == null ? playerName : null);
            List<String> mergedPlayerLore = override.playerLore != null ? override.playerLore : (override.lore == null ? playerLore : null);
            String mergedServerName = override.serverName != null ? override.serverName : (override.name == null ? serverName : null);
            List<String> mergedServerLore = override.serverLore != null ? override.serverLore : (override.lore == null ? serverLore : null);

            return new NoteVisual(
                    override.item == null ? item : override.item,
                    override.customModelDataSet == null ? customModelDataSet : override.customModelDataSet,
                    override.customModelData == null ? customModelData : override.customModelData,
                    override.dataSet == null ? dataSet : override.dataSet,
                    override.data == null ? data : override.data,
                    override.glowEnabled == null ? glowEnabled : override.glowEnabled,
                    override.glowColor == null ? glowColor : override.glowColor,
                    override.skullTexture == null ? skullTexture : override.skullTexture,
                    override.customName == null ? customName : override.customName,
                    override.flags == null ? flags : override.flags,
                    override.enchants == null ? enchants : override.enchants,
                    mergedName,
                    mergedLore,
                    mergedPlayerName,
                    mergedPlayerLore,
                    mergedServerName,
                    mergedServerLore,
                    override.withdrawSound == null ? withdrawSound : withdrawSound.with(override.withdrawSound),
                    override.redeemSound == null ? redeemSound : redeemSound.with(override.redeemSound));
        }

        public String getItem() {
            return item;
        }

        public boolean isCustomModelDataSet() {
            return customModelDataSet;
        }

        public int getCustomModelData() {
            return customModelData;
        }

        public boolean isDataSet() {
            return dataSet;
        }

        public int getData() {
            return data;
        }

        public boolean isGlowEnabled() {
            return glowEnabled;
        }

        public String getGlowColor() {
            return glowColor;
        }

        public String getSkullTexture() {
            return skullTexture;
        }

        public String getCustomName() {
            return customName;
        }

        public List<String> getFlags() {
            return flags == null ? new ArrayList<String>() : copyList(flags);
        }

        public Map<String, Integer> getEnchants() {
            return enchants == null ? new LinkedHashMap<String, Integer>() : copyMap(enchants);
        }

        public String getName(boolean signed) {
            if (signed && playerName != null) return playerName;
            if (!signed && serverName != null) return serverName;
            return name;
        }

        public List<String> getLore(boolean signed) {
            if (signed && playerLore != null) return copyList(playerLore);
            if (!signed && serverLore != null) return copyList(serverLore);
            return copyList(lore);
        }

        public SoundSettings getWithdrawSound() {
            return withdrawSound;
        }

        public SoundSettings getRedeemSound() {
            return redeemSound;
        }
    }

    public static final class SoundSettings {
        private final boolean enabled;
        private final String sound;
        private final float volume;
        private final float pitch;

        private SoundSettings(boolean enabled, String sound, float volume, float pitch) {
            this.enabled = enabled;
            this.sound = sound == null || sound.trim().isEmpty() ? "ENTITY_EXPERIENCE_ORB_PICKUP" : sound.trim();
            this.volume = volume;
            this.pitch = pitch;
        }

        private static SoundSettings disabled(String fallbackSound) {
            return new SoundSettings(false, fallbackSound, 1.0F, 1.0F);
        }

        private SoundSettings with(SoundOverride override) {
            if (override == null) return this;
            return new SoundSettings(
                    override.enabled == null ? enabled : override.enabled,
                    override.sound == null ? sound : override.sound,
                    override.volume == null ? volume : override.volume,
                    override.pitch == null ? pitch : override.pitch);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getSound() {
            return sound;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }
    }

    private static final class NoteOverride {
        private final String item;
        private final Boolean customModelDataSet;
        private final Integer customModelData;
        private final Boolean dataSet;
        private final Integer data;
        private final Boolean glowEnabled;
        private final String glowColor;
        private final String skullTexture;
        private final String customName;
        private final List<String> flags;
        private final Map<String, Integer> enchants;
        private final String name;
        private final List<String> lore;
        private final String playerName;
        private final List<String> playerLore;
        private final String serverName;
        private final List<String> serverLore;
        private final SoundOverride withdrawSound;
        private final SoundOverride redeemSound;

        private NoteOverride(
                String item,
                Boolean customModelDataSet,
                Integer customModelData,
                Boolean dataSet,
                Integer data,
                Boolean glowEnabled,
                String glowColor,
                String skullTexture,
                String customName,
                List<String> flags,
                Map<String, Integer> enchants,
                String name,
                List<String> lore,
                String playerName,
                List<String> playerLore,
                String serverName,
                List<String> serverLore,
                SoundOverride withdrawSound,
                SoundOverride redeemSound) {
            this.item = item;
            this.customModelDataSet = customModelDataSet;
            this.customModelData = customModelData;
            this.dataSet = dataSet;
            this.data = data;
            this.glowEnabled = glowEnabled;
            this.glowColor = glowColor;
            this.skullTexture = skullTexture;
            this.customName = customName;
            this.flags = copyList(flags);
            this.enchants = copyMap(enchants);
            this.name = name;
            this.lore = copyList(lore);
            this.playerName = playerName;
            this.playerLore = copyList(playerLore);
            this.serverName = serverName;
            this.serverLore = copyList(serverLore);
            this.withdrawSound = withdrawSound;
            this.redeemSound = redeemSound;
        }
    }

    private static final class SoundOverride {
        private final Boolean enabled;
        private final String sound;
        private final Float volume;
        private final Float pitch;

        private SoundOverride(Boolean enabled, String sound, Float volume, Float pitch) {
            this.enabled = enabled;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    private static final class IntegerOverride {
        private final boolean set;
        private final int value;

        private IntegerOverride(boolean set, int value) {
            this.set = set;
            this.value = value;
        }

        private static IntegerOverride set(int value) {
            return new IntegerOverride(true, value);
        }

        private static IntegerOverride clear() {
            return new IntegerOverride(false, 0);
        }
    }

    private static final class ValueLookup {
        private final boolean found;
        private final Object value;

        private ValueLookup(boolean found, Object value) {
            this.found = found;
            this.value = value;
        }

        private static ValueLookup found(Object value) {
            return new ValueLookup(true, value);
        }

        private static ValueLookup missing() {
            return new ValueLookup(false, null);
        }
    }
}
