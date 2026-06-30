package me.mraxetv.beastwithdraw.gui.depositor;

import me.mraxetv.beastlib.lib.tgui.gui.guis.BaseGui;
import me.mraxetv.beastlib.lib.tgui.gui.guis.GuiItem;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class DepositGuiOpenAnimationTask extends BukkitRunnable {

    private static final class AnimatedSlot {
        private final int slot;
        private final GuiItem item;

        private AnimatedSlot(int slot, GuiItem item) {
            this.slot = slot;
            this.item = item;
        }
    }

    private final BeastWithdrawPlugin plugin;
    private final Player player;
    private final BaseGui gui;
    private final DepositGuiAnimationSettings settings;
    private final List<AnimatedSlot> slots = new ArrayList<>();
    private final Map<Integer, GuiItem> finalItems;
    private final Set<Integer> finalEmptySlots;
    private final int rows;
    private int index;
    private int rowPauseRemaining;
    private int currentRow = -1;
    private int placedItems;

    DepositGuiOpenAnimationTask(BeastWithdrawPlugin plugin,
                                Player player,
                                BaseGui gui,
                                int rows,
                                Map<Integer, GuiItem> finalItems,
                                Set<Integer> finalEmptySlots,
                                DepositGuiAnimationSettings settings) {
        this.plugin = plugin;
        this.player = player;
        this.gui = gui;
        this.settings = settings;
        this.finalItems = finalItems;
        this.finalEmptySlots = finalEmptySlots == null ? new HashSet<>() : new HashSet<>(finalEmptySlots);
        this.rows = Math.max(1, rows);
        for (Map.Entry<Integer, GuiItem> entry : finalItems.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            this.slots.add(new AnimatedSlot(entry.getKey(), entry.getValue()));
        }
        for (Integer slot : this.finalEmptySlots) {
            if (slot != null) {
                this.slots.add(new AnimatedSlot(slot, null));
            }
        }
        orderSlots();
    }

    void start() {
        if (isStillViewingGui()) {
            playSound(settings.getStartSound(), settings.getStartVolume(), settings.getStartPitch());
        }
        runTaskTimer(plugin, settings.getStartDelayTicks(), settings.getTickDelay());
    }

    @Override
    public void run() {
        if (!isStillViewingGui()) {
            cancel();
            return;
        }

        if (rowPauseRemaining > 0) {
            rowPauseRemaining--;
            return;
        }

        int placedThisRun = 0;
        while (index < slots.size() && placedThisRun < settings.getItemsPerTick()) {
            AnimatedSlot slot = slots.get(index);
            int row = row(slot.slot);
            if (usesRowPause() && currentRow != -1 && row != currentRow && settings.getRowPauseTicks() > 0) {
                currentRow = row;
                rowPauseRemaining = Math.max(0, settings.getRowPauseTicks() - 1);
                playSound(settings.getRowSound(), settings.getRowVolume(), settings.getRowPitch());
                return;
            }

            currentRow = row;
            place(slot);
            index++;
            placedItems++;
            placedThisRun++;
        }

        if (index >= slots.size()) {
            playSound(settings.getFinishSound(), settings.getFinishVolume(), settings.getFinishPitch());
            finish();
            cancel();
        }
    }

    private void place(AnimatedSlot slot) {
        if (slot.item == null) {
            if (settings.isLockClicksUntilFinished()) {
                return;
            }
            gui.removeItem(slot.slot);
            gui.getInventory().setItem(slot.slot, null);
        } else {
            GuiItem displayItem = slot.item;
            if (settings.isLockClicksUntilFinished()) {
                ItemStack stack = slot.item.getItemStack();
                displayItem = stack == null ? slot.item : new GuiItem(stack.clone(), event -> event.setCancelled(true));
            }
            gui.updateItem(slot.slot, displayItem);
        }

        if ((placedItems + 1) % settings.getItemSoundEvery() == 0) {
            playSound(settings.getItemSound(), settings.getItemVolume(), itemPitch());
        }
    }

    private void finish() {
        for (Integer slot : finalEmptySlots) {
            if (gui.getGuiItems().containsKey(slot)) {
                gui.removeItem(slot);
                gui.getInventory().setItem(slot, null);
            }
        }
        for (Map.Entry<Integer, GuiItem> entry : finalItems.entrySet()) {
            gui.updateItem(entry.getKey(), entry.getValue());
        }
        gui.update();
    }

    private void orderSlots() {
        switch (settings.getType()) {
            case SLIDE_RIGHT_TO_LEFT:
                slots.sort(Comparator
                        .comparingInt((AnimatedSlot slot) -> row(slot.slot))
                        .thenComparing((AnimatedSlot left, AnimatedSlot right) -> Integer.compare(col(right.slot), col(left.slot))));
                break;
            case SNAKE:
                slots.sort((left, right) -> {
                    int rowCompare = Integer.compare(row(left.slot), row(right.slot));
                    if (rowCompare != 0) {
                        return rowCompare;
                    }
                    if (row(left.slot) % 2 == 0) {
                        return Integer.compare(col(left.slot), col(right.slot));
                    }
                    return Integer.compare(col(right.slot), col(left.slot));
                });
                break;
            case DIAGONAL_WAVE:
                slots.sort(Comparator
                        .comparingInt((AnimatedSlot slot) -> row(slot.slot) + col(slot.slot))
                        .thenComparingInt(slot -> row(slot.slot))
                        .thenComparingInt(slot -> col(slot.slot)));
                break;
            case CENTER_OUT:
                slots.sort(Comparator
                        .comparingDouble((AnimatedSlot slot) -> centerDistance(slot.slot))
                        .thenComparingInt(slot -> slot.slot));
                break;
            case SPIRAL:
                Map<Integer, Integer> spiralOrder = spiralOrder();
                slots.sort(Comparator
                        .comparingInt((AnimatedSlot slot) -> spiralOrder.getOrDefault(slot.slot, Integer.MAX_VALUE))
                        .thenComparingInt(slot -> slot.slot));
                break;
            case RANDOM_POP:
                Collections.shuffle(slots, new Random(System.nanoTime()));
                break;
            case RAIN:
                slots.sort(Comparator
                        .comparingInt((AnimatedSlot slot) -> col(slot.slot))
                        .thenComparingInt(slot -> row(slot.slot)));
                break;
            case SLIDE_LEFT_TO_RIGHT:
            default:
                slots.sort(Comparator.comparingInt(slot -> slot.slot));
                break;
        }
    }

    private boolean usesRowPause() {
        return settings.getType() == DepositGuiAnimationSettings.Type.SLIDE_LEFT_TO_RIGHT
                || settings.getType() == DepositGuiAnimationSettings.Type.SLIDE_RIGHT_TO_LEFT
                || settings.getType() == DepositGuiAnimationSettings.Type.SNAKE;
    }

    private int row(int slot) {
        return slot / 9;
    }

    private int col(int slot) {
        return slot % 9;
    }

    private double centerDistance(int slot) {
        double centerRow = (rows - 1) / 2.0D;
        double centerColumn = 4.0D;
        double rowDistance = row(slot) - centerRow;
        double columnDistance = col(slot) - centerColumn;
        return rowDistance * rowDistance + columnDistance * columnDistance;
    }

    private Map<Integer, Integer> spiralOrder() {
        Map<Integer, Integer> order = new HashMap<>();
        int top = 0;
        int bottom = rows - 1;
        int left = 0;
        int right = 8;
        int slotIndex = 0;

        while (top <= bottom && left <= right) {
            for (int column = left; column <= right; column++) {
                order.put(top * 9 + column, slotIndex++);
            }
            top++;

            for (int row = top; row <= bottom; row++) {
                order.put(row * 9 + right, slotIndex++);
            }
            right--;

            if (top <= bottom) {
                for (int column = right; column >= left; column--) {
                    order.put(bottom * 9 + column, slotIndex++);
                }
                bottom--;
            }

            if (left <= right) {
                for (int row = bottom; row >= top; row--) {
                    order.put(row * 9 + left, slotIndex++);
                }
                left++;
            }
        }

        return order;
    }

    private float itemPitch() {
        float pitch = settings.getItemPitch() + (placedItems * settings.getItemPitchStep());
        return Math.min(settings.getItemPitchMax(), pitch);
    }

    private boolean isStillViewingGui() {
        return player != null
                && gui != null
                && gui.getInventory() != null
                && gui.getInventory().getViewers().contains(player);
    }

    private void playSound(Sound sound, float volume, float pitch) {
        if (!settings.isSoundEnabled() || player == null || sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
