package me.mraxetv.beastwithdraw.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class CashRedeemEvent extends CustomRedeemEvent {

    public CashRedeemEvent(Player player, ItemStack item, double amount, String type, boolean offHand) {
        super(player, item, amount, type, offHand);
    }
}

