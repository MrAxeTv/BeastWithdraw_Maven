package me.mraxetv.beastwithdraw.gui.depositor;

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
import java.util.Optional;

final class DepositGuiItemFactory {
    private final BeastWithdrawPlugin plugin;

    DepositGuiItemFactory(BeastWithdrawPlugin plugin) {
        this.plugin = plugin;
    }

    GuiItem createLockedGuiItem(DepositGuiProfile profile, Player player, String path) {
        ItemStack itemStack = createItem(profile, player, path);
        if (itemStack == null) {
            return null;
        }
        return new GuiItem(itemStack, event -> event.setCancelled(true));
    }

    ItemStack createItem(DepositGuiProfile profile, Player player, String path) {
        Material material = resolveMaterial(profile.getString(path + ".Material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack itemStack = new ItemStack(material, clamp(profile.getInt(path + ".Amount", 1), 1, 64));

        if (profile.contains(path + ".Data") && !MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1)) {
            itemStack.setDurability((short) Math.max(0, profile.getInt(path + ".Data", 0)));
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        String displayName = profile.getString(path + ".DisplayName", profile.getString(path + ".Name", ""));
        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(profile.applyPlaceholders(player, displayName));
        }

        List<String> configuredLore = profile.getStringList(path + ".DisplayLore");
        if (configuredLore.isEmpty()) {
            configuredLore = profile.getStringList(path + ".Lore");
        }

        if (!configuredLore.isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (String line : configuredLore) {
                lore.add(profile.applyPlaceholders(player, line));
            }
            meta.setLore(lore);
        }

        if (profile.getBoolean(path + ".Glow", false)) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (profile.contains(path + ".CustomModelData")
                && MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) {
            meta.setCustomModelData(profile.getInt(path + ".CustomModelData", 0));
        }

        for (String flag : profile.getStringList(path + ".Flags")) {
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

    private Material resolveMaterial(String materialName) {
        if (materialName != null) {
            Optional<XMaterial> material = XMaterial.matchXMaterial(materialName);
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
