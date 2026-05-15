package me.mraxetv.beastwithdraw.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class CustomRedeemEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ItemStack item;
    private final double amount;
    private final String type;
    private final boolean offHand;
    private boolean cancelled;

    public CustomRedeemEvent(Player player, ItemStack item, double amount, String type, boolean offHand) {
        this.player = player;
        this.item = item;
        this.amount = amount;
        this.type = type;
        this.offHand = offHand;
    }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public boolean isOffHand() {
        return offHand;
    }
}

