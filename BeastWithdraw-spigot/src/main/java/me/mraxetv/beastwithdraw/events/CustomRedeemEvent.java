package me.mraxetv.beastwithdraw.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class CustomRedeemEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    private final Player player;
    @Getter
    private final ItemStack item;
    @Getter
    private final double amount;
    @Getter
    private final String type;
    @Getter
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
}

