package me.mraxetv.beastwithdraw.gui.withdraw;

import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.lib.tgui.gui.guis.GuiItem;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class WithdrawGuiItemFactory {
    private final BeastWithdrawPlugin plugin;

    WithdrawGuiItemFactory(BeastWithdrawPlugin plugin) {
        this.plugin = plugin;
    }

    GuiItem createLockedGuiItem(WithdrawGuiProfile profile, Player player, String path) {
        ItemStack itemStack = createItem(profile, player, path, null, null);
        if (itemStack == null) {
            return null;
        }
        return new GuiItem(itemStack, event -> event.setCancelled(true));
    }

    ItemStack createItem(WithdrawGuiProfile profile, Player player, String path) {
        return createItem(profile, player, path, null, null);
    }

    ItemStack createItem(WithdrawGuiProfile profile, Player player, String path, Map<String, String> replacements, Material materialFallback) {
        return createItem(profile, player, path, replacements, materialFallback, Double.NaN, false);
    }

    ItemStack createItem(WithdrawGuiProfile profile, Player player, String path, Map<String, String> replacements,
                         Material materialFallback, double selectedAmount, boolean maxSelected) {
        String overridePath = resolveAmountOverridePath(profile, path, selectedAmount, maxSelected);
        Material material = contains(profile, path, overridePath, "Material")
                ? resolveMaterial(getString(profile, path, overridePath, "Material", "GRAY_STAINED_GLASS_PANE"))
                : materialFallback;
        if (material == null) {
            material = resolveMaterial("GRAY_STAINED_GLASS_PANE");
        }
        ItemStack itemStack = new ItemStack(material, clamp(getInt(profile, path, overridePath, "Amount", 1), 1, 64));

        if (contains(profile, path, overridePath, "Data") && !MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1)) {
            itemStack.setDurability((short) Math.max(0, getInt(profile, path, overridePath, "Data", 0)));
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        String displayName = resolveDisplayName(profile, path, overridePath);
        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(apply(profile, player, displayName, replacements));
        }

        List<String> configuredLore = resolveLore(profile, path, overridePath);

        if (!configuredLore.isEmpty()) {
            List<String> lore = new ArrayList<>();
            boolean hideRequiredItemLore = shouldHideRequiredItemLore(replacements);
            for (String line : configuredLore) {
                if (hideRequiredItemLore && containsRequiredItemPlaceholder(line)) {
                    continue;
                }
                lore.add(apply(profile, player, line, replacements));
            }
            meta.setLore(lore);
        }

        if (getBoolean(profile, path, overridePath, "Glow", false)) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (contains(profile, path, overridePath, "CustomModelData")
                && MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) {
            meta.setCustomModelData(getInt(profile, path, overridePath, "CustomModelData", 0));
        }

        for (String flag : getStringList(profile, path, overridePath, "Flags")) {
            if (flag == null || flag.trim().isEmpty()) {
                continue;
            }
            try {
                meta.addItemFlags(ItemFlag.valueOf(flag.trim().toUpperCase().replace('-', '_').replace(' ', '_')));
            } catch (IllegalArgumentException ignored) {
            }
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private boolean shouldHideRequiredItemLore(Map<String, String> replacements) {
        if (replacements == null) {
            return false;
        }

        String enabled = replacements.get("%required_item_enabled%");
        return enabled != null && "false".equalsIgnoreCase(enabled.trim());
    }

    private boolean containsRequiredItemPlaceholder(String line) {
        if (line == null) {
            return false;
        }

        String normalized = line.toLowerCase(Locale.ENGLISH);
        return normalized.contains("%required_item%") || normalized.contains("%required_amount%");
    }

    private String apply(WithdrawGuiProfile profile, Player player, String value, Map<String, String> replacements) {
        String text = value == null ? "" : value;
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                text = text.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return profile.applyPlaceholders(player, text);
    }

    private String resolveAmountOverridePath(WithdrawGuiProfile profile, String path, double selectedAmount, boolean maxSelected) {
        if (Double.isNaN(selectedAmount) || Double.isInfinite(selectedAmount)) {
            return null;
        }

        String basePath = path + ".AmountOverrides";
        if (!profile.getBoolean(basePath + ".Enabled", false)) {
            return null;
        }

        if (maxSelected && profile.isSection(basePath + ".Max")) {
            return basePath + ".Max";
        }

        if (!profile.isSection(basePath + ".Ranges")) {
            return null;
        }

        for (Object rawKey : profile.getSection(basePath + ".Ranges").getKeys()) {
            String key = String.valueOf(rawKey);
            String rangePath = basePath + ".Ranges." + key;
            double minimum = profile.contains(rangePath + ".Min")
                    ? profile.getDouble(rangePath + ".Min", Double.NEGATIVE_INFINITY)
                    : Double.NEGATIVE_INFINITY;
            double maximum = profile.contains(rangePath + ".Max")
                    ? profile.getDouble(rangePath + ".Max", Double.POSITIVE_INFINITY)
                    : Double.POSITIVE_INFINITY;

            if (selectedAmount >= minimum && selectedAmount <= maximum) {
                return rangePath;
            }
        }
        return null;
    }

    private boolean contains(WithdrawGuiProfile profile, String path, String overridePath, String key) {
        return (overridePath != null && profile.contains(overridePath + "." + key))
                || profile.contains(path + "." + key);
    }

    private String getString(WithdrawGuiProfile profile, String path, String overridePath, String key, String defaultValue) {
        if (overridePath != null && profile.contains(overridePath + "." + key)) {
            return profile.getString(overridePath + "." + key, defaultValue);
        }
        return profile.getString(path + "." + key, defaultValue);
    }

    private int getInt(WithdrawGuiProfile profile, String path, String overridePath, String key, int defaultValue) {
        if (overridePath != null && profile.contains(overridePath + "." + key)) {
            return profile.getInt(overridePath + "." + key, defaultValue);
        }
        return profile.getInt(path + "." + key, defaultValue);
    }

    private boolean getBoolean(WithdrawGuiProfile profile, String path, String overridePath, String key, boolean defaultValue) {
        if (overridePath != null && profile.contains(overridePath + "." + key)) {
            return profile.getBoolean(overridePath + "." + key, defaultValue);
        }
        return profile.getBoolean(path + "." + key, defaultValue);
    }

    private List<String> getStringList(WithdrawGuiProfile profile, String path, String overridePath, String key) {
        if (overridePath != null && profile.contains(overridePath + "." + key)) {
            return profile.getStringList(overridePath + "." + key);
        }
        return profile.getStringList(path + "." + key);
    }

    private String resolveDisplayName(WithdrawGuiProfile profile, String path, String overridePath) {
        if (overridePath != null && profile.contains(overridePath + ".DisplayName")) {
            return profile.getString(overridePath + ".DisplayName", "");
        }
        if (overridePath != null && profile.contains(overridePath + ".Name")) {
            return profile.getString(overridePath + ".Name", "");
        }
        return profile.getString(path + ".DisplayName", profile.getString(path + ".Name", ""));
    }

    private List<String> resolveLore(WithdrawGuiProfile profile, String path, String overridePath) {
        if (overridePath != null && profile.contains(overridePath + ".DisplayLore")) {
            return profile.getStringList(overridePath + ".DisplayLore");
        }
        if (overridePath != null && profile.contains(overridePath + ".Lore")) {
            return profile.getStringList(overridePath + ".Lore");
        }

        List<String> lore = profile.getStringList(path + ".DisplayLore");
        return lore.isEmpty() ? profile.getStringList(path + ".Lore") : lore;
    }

    private Material resolveMaterial(String materialName) {
        if (materialName != null) {
            Optional<XMaterial> material = XMaterial.matchXMaterial(materialName.trim().toUpperCase(Locale.ENGLISH));
            if (material.isPresent() && material.get().parseMaterial() != null) {
                return material.get().parseMaterial();
            }
        }

        Material fallback = XMaterial.GRAY_STAINED_GLASS_PANE.parseMaterial();
        return fallback == null ? Material.STONE : fallback;
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
