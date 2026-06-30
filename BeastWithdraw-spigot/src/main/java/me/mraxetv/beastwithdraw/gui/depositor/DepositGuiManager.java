package me.mraxetv.beastwithdraw.gui.depositor;

import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;
import me.mraxetv.beastlib.lib.kyori.adventure.text.Component;
import me.mraxetv.beastlib.lib.tgui.gui.guis.Gui;
import me.mraxetv.beastlib.lib.tgui.gui.guis.GuiItem;
import me.mraxetv.beastlib.lib.tgui.gui.guis.StorageGui;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DepositGuiManager {
    private static final String GUI_BASE = "GUI";
    private static final String EMPTY_SLOT_BASE = GUI_BASE + ".EmptySlot";

    private final BeastWithdrawPlugin plugin;
    private final DepositService depositService;
    private final DepositGuiItemFactory itemFactory;
    private final DepositGuiProfile mainProfile;
    private final DepositGuiSoundSettings soundSettings;
    private final DepositGuiAnimationSettings animationSettings;
    private final List<DepositGuiProfile> commandProfiles;
    private final Set<UUID> invalidMessageCooldown = new HashSet<>();
    private final Set<UUID> depositActionCooldown = new HashSet<>();

    public DepositGuiManager(BeastWithdrawPlugin plugin) {
        this.plugin = plugin;
        this.depositService = new DepositService(plugin);
        this.itemFactory = new DepositGuiItemFactory(plugin);
        this.mainProfile = DepositGuiProfile.main(plugin);
        this.soundSettings = DepositGuiSoundSettings.from(plugin, mainProfile);
        this.animationSettings = DepositGuiAnimationSettings.from(plugin, mainProfile);
        this.commandProfiles = buildCommandProfiles();
    }

    public boolean isEnabled() {
        return mainProfile.isEnabled();
    }

    public List<DepositGuiProfile> getCommandProfiles() {
        return new ArrayList<>(commandProfiles);
    }

    public void open(Player player) {
        open(player, mainProfile);
    }

    public void open(Player player, DepositGuiProfile profile) {
        int size = normalizeSize(profile.getInt(GUI_BASE + ".Size", 54));
        int rows = size / 9;
        List<Integer> depositSlots = getDepositSlots(profile, size);
        Set<Integer> depositSlotSet = new LinkedHashSet<>(depositSlots);

        StorageGui gui = Gui.storage()
                .title(Component.text(profile.applyPlaceholders(player, profile.getString(GUI_BASE + ".Title", "&8Note Depositor"))))
                .rows(rows)
                .create();

        Map<Integer, GuiItem> backgroundItems = applyFillers(profile, player, gui, rows);
        for (Integer slot : depositSlots) {
            gui.removeItem(slot);
            gui.getInventory().setItem(slot, null);
            backgroundItems.remove(slot);
        }
        applyConfiguredButton(profile, player, gui, GUI_BASE + ".Items.DepositButton", event -> {
            event.setCancelled(true);
            if (!beginDepositAction(player)) {
                return;
            }
            soundSettings.playClick(player);

            DepositService.DepositResult result = depositService.deposit(player, gui.getInventory(), depositSlots, profile);
            sendDepositResult(player, result);
            gui.update();

            if (result.hasDeposited() && profile.getBoolean(GUI_BASE + ".CloseAfterDeposit", true)) {
                gui.close(player);
            }
        });
        applyConfiguredButton(profile, player, gui, GUI_BASE + ".Items.CloseButton", event -> {
            event.setCancelled(true);
            soundSettings.playClick(player);
            gui.close(player);
        });

        gui.setDefaultTopClickAction(event -> handleTopClick(profile, player, event, depositSlotSet, size));
        gui.setPlayerInventoryAction(event -> handlePlayerInventoryClick(profile, player, event, depositSlots));
        gui.setDragAction(event -> handleDrag(profile, player, event, depositSlotSet, size));
        gui.setCloseGuiAction(event -> returnDepositItems(profile, player, event.getInventory(), depositSlots));
        openManagedGui(profile, player, gui, rows, backgroundItems);

        sendMessage(player, "Depositor.Opened", "%prefix% &ePlace notes in the depositor and click deposit.");
    }

    private List<DepositGuiProfile> buildCommandProfiles() {
        List<DepositGuiProfile> profiles = new ArrayList<>();
        if (mainProfile.isEnabled()) {
            profiles.add(mainProfile);
        }

        if (plugin.getWithdrawManager() == null) {
            return profiles;
        }

        for (String handlerId : plugin.getWithdrawManager().getAssetHandlerList()) {
            AssetHandler assetHandler = plugin.getWithdrawManager().getAssetHandler(handlerId);
            if (assetHandler == null) {
                continue;
            }

            DepositGuiProfile profile = DepositGuiProfile.asset(plugin, assetHandler);
            if (profile.isEnabled()) {
                profiles.add(profile);
            }
        }
        return profiles;
    }

    private Map<Integer, GuiItem> applyFillers(DepositGuiProfile profile, Player player, StorageGui gui, int rows) {
        if (!profile.getBoolean(EMPTY_SLOT_BASE + ".Enabled", true) || !profile.isSection(EMPTY_SLOT_BASE + ".Items")) {
            return new HashMap<>();
        }

        Section itemsSection = profile.getSection(EMPTY_SLOT_BASE + ".Items");
        List<GuiItem> fillerItems = new ArrayList<>();
        for (Object rawKey : itemsSection.getKeys()) {
            String key = String.valueOf(rawKey);
            GuiItem item = itemFactory.createLockedGuiItem(profile, player, EMPTY_SLOT_BASE + ".Items." + key);
            if (item != null) {
                fillerItems.add(item);
            }
        }

        Set<Integer> beforeSlots = new HashSet<>(gui.getGuiItems().keySet());
        DepositFillPattern.from(profile, EMPTY_SLOT_BASE, rows).apply(gui, fillerItems);
        Map<Integer, GuiItem> backgroundItems = new HashMap<>();
        for (Map.Entry<Integer, GuiItem> entry : gui.getGuiItems().entrySet()) {
            if (beforeSlots.contains(entry.getKey())) {
                continue;
            }
            backgroundItems.put(entry.getKey(), entry.getValue());
        }
        return backgroundItems;
    }

    private void openManagedGui(DepositGuiProfile profile, Player player, StorageGui gui, int rows,
                                Map<Integer, GuiItem> backgroundItems) {
        if (!animationSettings.isEnabled()) {
            gui.open(player);
            soundSettings.playMenuOpen(player);
            return;
        }

        Map<Integer, GuiItem> finalItems = new LinkedHashMap<>(gui.getGuiItems());
        Map<Integer, GuiItem> animatedItems = new LinkedHashMap<>();
        Set<Integer> animatedEmptySlots = new LinkedHashSet<>();
        GuiItem placeholderItem = getAnimationPlaceholderItem(profile, player);
        int size = gui.getInventory().getSize();

        for (int slot = 0; slot < size; slot++) {
            GuiItem item = finalItems.get(slot);
            boolean hasItem = item != null;
            boolean shouldAnimate = animationSettings.isAnimateEmptySlots()
                    || (hasItem && !isBackgroundItem(slot, item, backgroundItems));
            if (!shouldAnimate) {
                continue;
            }

            if (hasItem) {
                animatedItems.put(slot, item);
            } else if (placeholderItem != null) {
                animatedEmptySlots.add(slot);
            }

            if (placeholderItem == null) {
                gui.removeItem(slot);
                gui.getInventory().setItem(slot, null);
            } else {
                gui.setItem(slot, placeholderItem);
            }
        }

        gui.open(player);
        soundSettings.playMenuOpen(player);

        if (!animatedItems.isEmpty() || !animatedEmptySlots.isEmpty()) {
            new DepositGuiOpenAnimationTask(plugin, player, gui, rows, animatedItems, animatedEmptySlots, animationSettings).start();
        }
    }

    private boolean isBackgroundItem(Integer slot, GuiItem item, Map<Integer, GuiItem> backgroundItems) {
        if (slot == null || backgroundItems == null || backgroundItems.isEmpty()) {
            return false;
        }
        return backgroundItems.get(slot) == item;
    }

    private GuiItem getAnimationPlaceholderItem(DepositGuiProfile profile, Player player) {
        if (!animationSettings.isPlaceholderItemEnabled()) {
            return null;
        }
        return itemFactory.createLockedGuiItem(profile, player, "Animations.Open.PlaceholderItem");
    }

    private void applyConfiguredButton(DepositGuiProfile profile, Player player, StorageGui gui, String path, me.mraxetv.beastlib.lib.tgui.gui.components.GuiAction<InventoryClickEvent> action) {
        if (!profile.getBoolean(path + ".Enabled", true)) {
            return;
        }

        int slot = configSlotToIndex(profile.getInt(path + ".Slot", -1), gui.getInventory().getSize());
        if (slot < 0) {
            return;
        }

        ItemStack itemStack = itemFactory.createItem(profile, player, path);
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        gui.setItem(slot, new GuiItem(itemStack, action));
    }

    private void handleTopClick(DepositGuiProfile profile, Player player, InventoryClickEvent event, Set<Integer> depositSlots, int topSize) {
        int slot = event.getSlot();
        if (!depositSlots.contains(slot)) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            ItemStack hotbarItem = event.getHotbarButton() >= 0
                    ? player.getInventory().getItem(event.getHotbarButton())
                    : null;
            if (!isEmpty(hotbarItem) && !depositService.isDepositNote(hotbarItem, profile)) {
                event.setCancelled(true);
                sendRejectedItemMessage(profile, player, hotbarItem);
            }
            return;
        }

        ItemStack cursor = event.getCursor();
        if (placesItem(event.getAction()) && !isEmpty(cursor) && !depositService.isDepositNote(cursor, profile)) {
            event.setCancelled(true);
            sendRejectedItemMessage(profile, player, cursor);
            return;
        }

        if (event.getRawSlot() >= topSize) {
            event.setCancelled(true);
        }
    }

    private void handlePlayerInventoryClick(DepositGuiProfile profile, Player player, InventoryClickEvent event, List<Integer> depositSlots) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (isEmpty(clicked)) {
            return;
        }

        if (!depositService.isDepositNote(clicked, profile)) {
            event.setCancelled(true);
            sendRejectedItemMessage(profile, player, clicked);
            return;
        }

        event.setCancelled(true);
        int originalAmount = clicked.getAmount();
        ItemStack remaining = moveToDepositSlots(event.getView().getTopInventory(), depositSlots, clicked);
        if (isEmpty(remaining)) {
            event.setCurrentItem(null);
            return;
        }

        event.setCurrentItem(remaining);
        if (remaining.getAmount() == originalAmount) {
            sendMessage(player, "Depositor.Full", "%prefix% &cThe depositor GUI is full.");
        }
    }

    private ItemStack moveToDepositSlots(Inventory inventory, List<Integer> depositSlots, ItemStack source) {
        ItemStack remaining = source.clone();
        fillSimilarDepositStacks(inventory, depositSlots, remaining);
        if (isEmpty(remaining)) {
            return null;
        }

        fillEmptyDepositSlots(inventory, depositSlots, remaining);
        return isEmpty(remaining) ? null : remaining;
    }

    private void fillSimilarDepositStacks(Inventory inventory, List<Integer> depositSlots, ItemStack remaining) {
        for (Integer slot : depositSlots) {
            if (isEmpty(remaining) || slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            ItemStack current = inventory.getItem(slot);
            if (isEmpty(current) || !current.isSimilar(remaining)) {
                continue;
            }

            int maxStack = Math.min(current.getMaxStackSize(), remaining.getMaxStackSize());
            int space = maxStack - current.getAmount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getAmount());
            current.setAmount(current.getAmount() + moved);
            remaining.setAmount(remaining.getAmount() - moved);
        }
    }

    private void fillEmptyDepositSlots(Inventory inventory, List<Integer> depositSlots, ItemStack remaining) {
        for (Integer slot : depositSlots) {
            if (isEmpty(remaining) || slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            if (!isEmpty(inventory.getItem(slot))) {
                continue;
            }

            int moved = Math.min(remaining.getMaxStackSize(), remaining.getAmount());
            ItemStack placed = remaining.clone();
            placed.setAmount(moved);
            inventory.setItem(slot, placed);
            remaining.setAmount(remaining.getAmount() - moved);
        }
    }

    private void handleDrag(DepositGuiProfile profile, Player player, InventoryDragEvent event, Set<Integer> depositSlots, int topSize) {
        boolean touchesTop = false;
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot == null || rawSlot >= topSize) {
                continue;
            }

            touchesTop = true;
            if (!depositSlots.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        if (touchesTop && !isEmpty(event.getOldCursor()) && !depositService.isDepositNote(event.getOldCursor(), profile)) {
            event.setCancelled(true);
            sendRejectedItemMessage(profile, player, event.getOldCursor());
        }
    }

    private void returnDepositItems(DepositGuiProfile profile, Player player, Inventory inventory, List<Integer> depositSlots) {
        ItemStack animationPlaceholder = createAnimationPlaceholderStack(profile, player);
        for (Integer slot : depositSlots) {
            if (slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            ItemStack itemStack = inventory.getItem(slot);
            if (isEmpty(itemStack)) {
                continue;
            }

            inventory.setItem(slot, null);
            if (isAnimationPlaceholder(itemStack, animationPlaceholder)) {
                continue;
            }
            player.getInventory().addItem(itemStack).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private ItemStack createAnimationPlaceholderStack(DepositGuiProfile profile, Player player) {
        if (!animationSettings.isPlaceholderItemEnabled()) {
            return null;
        }
        return itemFactory.createItem(profile, player, "Animations.Open.PlaceholderItem");
    }

    private boolean isAnimationPlaceholder(ItemStack itemStack, ItemStack animationPlaceholder) {
        return !isEmpty(itemStack)
                && !isEmpty(animationPlaceholder)
                && itemStack.isSimilar(animationPlaceholder);
    }

    private void sendDepositResult(Player player, DepositService.DepositResult result) {
        if (result.hasDeposited()) {
            for (DepositService.DepositSummary summary : result.getSummaries()) {
                sendSummary(player, summary);
            }
        } else {
            sendMessage(player, "Depositor.NoNotes", "%prefix% &cNo valid notes were deposited.");
        }

        if (result.getInvalidItems() > 0) {
            sendCountMessage(player, "Depositor.SkippedInvalid", "%prefix% &cSkipped %items% invalid item(s).", result.getInvalidItems());
        }
        if (result.getUnsupportedItems() > 0) {
            sendCountMessage(player, "Depositor.SkippedUnsupported", "%prefix% &cSkipped %items% note(s) because this depositor does not accept that type.", result.getUnsupportedItems());
        }
        if (result.getNoPermissionItems() > 0) {
            sendCountMessage(player, "Depositor.SkippedNoPermission", "%prefix% &cSkipped %items% note(s) because you lack redeem permission.", result.getNoPermissionItems());
        }
        if (result.getFailedItems() > 0) {
            sendCountMessage(player, "Depositor.SkippedFailed", "%prefix% &cSkipped %items% note(s) because the transaction failed.", result.getFailedItems());
        }
        if (result.getCapacityItems() > 0) {
            sendCountMessage(player, "Depositor.SkippedCapacity", "%prefix% &cSkipped %items% note(s) because your balance is already full.", result.getCapacityItems());
        }
    }

    private void sendSummary(Player player, DepositService.DepositSummary summary) {
        AssetHandler assetHandler = summary.getAssetHandler();
        String path = summary.getTotalTax() > 0 ? "Depositor.DepositedAndTax" : "Depositor.Deposited";
        String fallback = summary.getTotalTax() > 0
                ? "%prefix% &eDeposited &b%notes%x &7%type%&8: &a+%amount% &cTax: &f-%tax% &8| &7Balance: &b%balance%"
                : "%prefix% &eDeposited &b%notes%x &7%type%&8: &a+%amount% &8| &7Balance: &b%balance%";
        String message = getMessage(path, fallback);
        message = message.replace("%type%", assetHandler.getConfigName());
        message = message.replace("%notes%", String.valueOf(summary.getNotes()));
        message = message.replace("%items%", String.valueOf(summary.getNotes()));
        message = message.replace("%total%", assetHandler.formatWithPreSuffix(summary.getTotalAmount()));
        message = message.replace("%tax%", assetHandler.formatWithPreSuffix(summary.getTotalTax()));
        message = message.replace("%amount%", assetHandler.formatWithPreSuffix(summary.getFinalAmount()));
        message = message.replace("%balance%", assetHandler.formatWithPreSuffix(summary.getBalanceAfter()));
        message = assetHandler.applyPlaceholders(message, player);
        plugin.getUtils().sendMessage(player, message);
    }

    private void sendCountMessage(CommandSender sender, String path, String fallback, int items) {
        String message = getMessage(path, fallback).replace("%items%", String.valueOf(items));
        plugin.getUtils().sendMessage(sender, message);
    }

    private void sendInvalidItemMessage(Player player) {
        sendCooldownMessage(player, "Depositor.InvalidItem", "%prefix% &cOnly BeastWithdraw notes can be placed in this GUI.");
    }

    private void sendUnsupportedItemMessage(Player player) {
        sendCooldownMessage(player, "Depositor.UnsupportedItem", "%prefix% &cThis depositor does not accept that note type.");
    }

    private void sendRejectedItemMessage(DepositGuiProfile profile, Player player, ItemStack itemStack) {
        DepositService.NoteData noteData = depositService.readNote(itemStack);
        if (noteData != null && !profile.accepts(noteData.getAssetHandler())) {
            sendUnsupportedItemMessage(player);
            return;
        }

        sendInvalidItemMessage(player);
    }

    private void sendCooldownMessage(Player player, String path, String fallback) {
        UUID uuid = player.getUniqueId();
        if (!invalidMessageCooldown.add(uuid)) {
            return;
        }

        sendMessage(player, path, fallback);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> invalidMessageCooldown.remove(uuid), 1L);
    }

    private boolean beginDepositAction(Player player) {
        UUID uuid = player.getUniqueId();
        if (!depositActionCooldown.add(uuid)) {
            return false;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> depositActionCooldown.remove(uuid), 2L);
        return true;
    }

    private void sendMessage(CommandSender sender, String path, String fallback) {
        plugin.getUtils().sendMessage(sender, getMessage(path, fallback));
    }

    private String getMessage(String path, String fallback) {
        return plugin.getMessages().contains(path) ? plugin.getMessages().getString(path) : fallback;
    }

    private List<Integer> getDepositSlots(DepositGuiProfile profile, int size) {
        List<Integer> configured = profile.getIntList(GUI_BASE + ".DepositSlots");
        if (configured.isEmpty()) {
            configured = defaultDepositSlots();
        } else if (isLegacyDefaultDepositSlots(configured)) {
            configured = defaultDepositSlots();
        }

        Set<Integer> slots = new LinkedHashSet<>();
        for (Integer slot : configured) {
            int normalized = configSlotToIndex(slot == null ? -1 : slot, size);
            if (normalized >= 0) {
                slots.add(normalized);
            }
        }
        return new ArrayList<>(slots);
    }

    private List<Integer> defaultDepositSlots() {
        List<Integer> slots = new ArrayList<>();
        slots.add(11);
        slots.add(12);
        slots.add(13);
        slots.add(14);
        slots.add(15);
        slots.add(16);
        slots.add(17);
        slots.add(20);
        slots.add(21);
        slots.add(22);
        slots.add(23);
        slots.add(24);
        slots.add(25);
        slots.add(26);
        slots.add(29);
        slots.add(30);
        slots.add(31);
        slots.add(32);
        slots.add(33);
        slots.add(34);
        slots.add(35);
        return slots;
    }

    private boolean isLegacyDefaultDepositSlots(List<Integer> slots) {
        if (slots == null || slots.size() != 18) {
            return false;
        }

        int[] legacySlots = {
                11, 12, 13, 14, 15, 16,
                20, 21, 22, 23, 24, 25,
                29, 30, 31, 32, 33, 34
        };
        for (int index = 0; index < legacySlots.length; index++) {
            Integer slot = slots.get(index);
            if (slot == null || slot.intValue() != legacySlots[index]) {
                return false;
            }
        }
        return true;
    }

    private int configSlotToIndex(int slot, int size) {
        int normalized = slot - 1;
        return normalized >= 0 && normalized < size ? normalized : -1;
    }

    private int normalizeSize(int configuredSize) {
        if (configuredSize < 9) {
            return 9;
        }
        if (configuredSize > 54) {
            return 54;
        }
        return (configuredSize / 9) * 9;
    }

    private boolean placesItem(InventoryAction action) {
        return action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME
                || action == InventoryAction.SWAP_WITH_CURSOR;
    }

    private boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0;
    }
}
