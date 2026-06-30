package me.mraxetv.beastwithdraw.managers;

import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class WithdrawItemRequirement {
    private static final String GLOBAL_BASE = "RequiredItem";
    private static final String TYPE_BASE = "Settings.RequiredItem";

    private final BeastWithdrawPlugin plugin;
    private final AssetHandler assetHandler;
    private final boolean enabled;
    private final boolean consume;
    private final boolean amountPerNote;
    private final int amount;
    private final Material material;
    private final int data;
    private final boolean dataSet;
    private final String displayName;
    private final boolean requireDisplayName;
    private final List<String> lore;
    private final boolean requireLore;
    private final int customModelData;
    private final boolean requireCustomModelData;

    private WithdrawItemRequirement(BeastWithdrawPlugin plugin, AssetHandler assetHandler) {
        this.plugin = plugin;
        this.assetHandler = assetHandler;
        this.enabled = getBoolean("Enabled", false);
        this.consume = getBoolean("Consume", true);
        this.amountPerNote = getBoolean("AmountPerNote", true);
        this.amount = Math.max(1, getInt("Amount", 1));
        this.material = resolveMaterial(getString("Match.Material", "PAPER"));
        this.data = Math.max(0, getInt("Match.Data", 0));
        this.dataSet = contains("Match.Data");
        this.displayName = getString("Match.DisplayName", "");
        this.requireDisplayName = getBoolean("Match.RequireDisplayName", displayName != null && !displayName.trim().isEmpty());
        this.lore = getStringList("Match.Lore");
        this.requireLore = getBoolean("Match.RequireLore", !lore.isEmpty());
        this.customModelData = Math.max(0, getInt("Match.CustomModelData", 0));
        this.requireCustomModelData = getBoolean("Match.RequireCustomModelData", customModelData > 0);
    }

    public static WithdrawItemRequirement from(BeastWithdrawPlugin plugin, AssetHandler assetHandler) {
        return new WithdrawItemRequirement(plugin, assetHandler);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConsume() {
        return consume;
    }

    public int getRequiredAmount(int noteStackSize) {
        return amount * (amountPerNote ? Math.max(1, noteStackSize) : 1);
    }

    public String getDisplayName() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        return material.name();
    }

    public boolean has(ItemSource source, int noteStackSize) {
        if (!enabled) {
            return true;
        }
        return countMatching(source) >= getRequiredAmount(noteStackSize);
    }

    public ConsumeResult consume(Player player, ItemSource source, int noteStackSize) {
        if (!enabled) {
            return ConsumeResult.notRequired();
        }

        int requiredAmount = getRequiredAmount(noteStackSize);
        if (countMatching(source) < requiredAmount) {
            sendMissingMessage(player, requiredAmount);
            return ConsumeResult.failed();
        }

        if (!consume) {
            return ConsumeResult.notConsumed();
        }

        int remaining = requiredAmount;
        List<ItemStack> consumed = new ArrayList<>();
        for (int index = 0; index < source.size() && remaining > 0; index++) {
            ItemStack itemStack = source.getItem(index);
            if (!matches(itemStack)) {
                continue;
            }

            int taken = Math.min(remaining, itemStack.getAmount());
            ItemStack consumedPart = itemStack.clone();
            consumedPart.setAmount(taken);
            consumed.add(consumedPart);

            int left = itemStack.getAmount() - taken;
            if (left <= 0) {
                source.setItem(index, null);
            } else {
                itemStack.setAmount(left);
                source.setItem(index, itemStack);
            }
            remaining -= taken;
        }

        return new ConsumeResult(true, consumed);
    }

    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0) {
            return false;
        }

        if (material != null && itemStack.getType() != material) {
            return false;
        }

        if (dataSet && itemStack.getDurability() != (short) data) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (requireDisplayName) {
            String expected = Utils.setColor(displayName == null ? "" : displayName);
            if (meta == null || !meta.hasDisplayName() || !expected.equals(meta.getDisplayName())) {
                return false;
            }
        }

        if (requireLore) {
            List<String> expectedLore = color(lore);
            if (meta == null || !meta.hasLore() || !expectedLore.equals(meta.getLore())) {
                return false;
            }
        }

        if (requireCustomModelData && MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) {
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
                return false;
            }
        }

        return true;
    }

    private int countMatching(ItemSource source) {
        if (source == null) {
            return 0;
        }

        int count = 0;
        for (int index = 0; index < source.size(); index++) {
            ItemStack itemStack = source.getItem(index);
            if (matches(itemStack)) {
                count += itemStack.getAmount();
            }
        }
        return count;
    }

    private void sendMissingMessage(Player player, int requiredAmount) {
        if (player == null) {
            return;
        }

        String message = plugin.getMessages().contains("Withdraws.RequiredItem.Missing")
                ? plugin.getMessages().getString("Withdraws.RequiredItem.Missing")
                : "%prefix% &cYou need &e%amount%x %item% &cto withdraw this note.";
        message = message.replace("%amount%", String.valueOf(requiredAmount));
        message = message.replace("%item%", getDisplayName());
        message = assetHandler.applyPlaceholders(message, player);
        plugin.getUtils().sendMessage(player, message);
    }

    private boolean contains(String path) {
        return typeOverrides() && assetHandler.getConfig().contains(TYPE_BASE + "." + path)
                || (plugin.getWithdrawSettings() != null && plugin.getWithdrawSettings().contains(GLOBAL_BASE + "." + path));
    }

    private boolean getBoolean(String path, boolean defaultValue) {
        if (typeOverrides() && assetHandler.getConfig().contains(TYPE_BASE + "." + path)) {
            return assetHandler.getConfig().getBoolean(TYPE_BASE + "." + path, defaultValue);
        }
        YamlDocument global = plugin.getWithdrawSettings();
        return global == null ? defaultValue : global.getBoolean(GLOBAL_BASE + "." + path, defaultValue);
    }

    private int getInt(String path, int defaultValue) {
        if (typeOverrides() && assetHandler.getConfig().contains(TYPE_BASE + "." + path)) {
            return assetHandler.getConfig().getInt(TYPE_BASE + "." + path, defaultValue);
        }
        YamlDocument global = plugin.getWithdrawSettings();
        return global == null ? defaultValue : global.getInt(GLOBAL_BASE + "." + path, defaultValue);
    }

    private String getString(String path, String defaultValue) {
        if (typeOverrides() && assetHandler.getConfig().contains(TYPE_BASE + "." + path)) {
            return assetHandler.getConfig().getString(TYPE_BASE + "." + path, defaultValue);
        }
        YamlDocument global = plugin.getWithdrawSettings();
        return global == null ? defaultValue : global.getString(GLOBAL_BASE + "." + path, defaultValue);
    }

    private List<String> getStringList(String path) {
        if (typeOverrides() && assetHandler.getConfig().contains(TYPE_BASE + "." + path)) {
            return assetHandler.getConfig().getStringList(TYPE_BASE + "." + path);
        }
        YamlDocument global = plugin.getWithdrawSettings();
        return global == null ? Collections.emptyList() : global.getStringList(GLOBAL_BASE + "." + path);
    }

    private boolean typeOverrides() {
        return assetHandler.getConfig().getBoolean(TYPE_BASE + ".OverrideGlobal", false);
    }

    private Material resolveMaterial(String materialName) {
        if (materialName != null) {
            Optional<XMaterial> material = XMaterial.matchXMaterial(materialName);
            if (material.isPresent() && material.get().parseMaterial() != null) {
                return material.get().parseMaterial();
            }
        }
        Material fallback = XMaterial.PAPER.parseMaterial();
        return fallback == null ? Material.STONE : fallback;
    }

    private List<String> color(List<String> lines) {
        List<String> result = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                result.add(Utils.setColor(line == null ? "" : line));
            }
        }
        return result;
    }

    public interface ItemSource {
        int size();

        ItemStack getItem(int index);

        void setItem(int index, ItemStack itemStack);
    }

    public static ItemSource playerInventory(Player player) {
        return new ItemSource() {
            private final PlayerInventory inventory = player.getInventory();

            @Override
            public int size() {
                return inventory.getSize();
            }

            @Override
            public ItemStack getItem(int index) {
                return inventory.getItem(index);
            }

            @Override
            public void setItem(int index, ItemStack itemStack) {
                inventory.setItem(index, itemStack);
            }
        };
    }

    public static ItemSource inventorySlots(Inventory inventory, List<Integer> slots) {
        return new ItemSource() {
            @Override
            public int size() {
                return slots == null ? 0 : slots.size();
            }

            @Override
            public ItemStack getItem(int index) {
                int slot = slots.get(index);
                return slot < 0 || slot >= inventory.getSize() ? null : inventory.getItem(slot);
            }

            @Override
            public void setItem(int index, ItemStack itemStack) {
                int slot = slots.get(index);
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, itemStack);
                }
            }
        };
    }

    public static final class ConsumeResult {
        private final boolean success;
        private final List<ItemStack> consumedItems;

        private ConsumeResult(boolean success, List<ItemStack> consumedItems) {
            this.success = success;
            this.consumedItems = consumedItems == null ? Collections.emptyList() : consumedItems;
        }

        private static ConsumeResult failed() {
            return new ConsumeResult(false, Collections.emptyList());
        }

        private static ConsumeResult notRequired() {
            return new ConsumeResult(true, Collections.emptyList());
        }

        private static ConsumeResult notConsumed() {
            return new ConsumeResult(true, Collections.emptyList());
        }

        public boolean isSuccess() {
            return success;
        }

        public void rollback(Player player) {
            if (player == null || consumedItems.isEmpty()) {
                return;
            }

            for (ItemStack itemStack : consumedItems) {
                player.getInventory().addItem(itemStack).values()
                        .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
        }
    }
}
