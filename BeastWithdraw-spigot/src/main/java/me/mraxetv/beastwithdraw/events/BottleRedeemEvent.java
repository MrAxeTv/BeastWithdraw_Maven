package me.mraxetv.beastwithdraw.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BottleRedeemEvent extends CustomRedeemEvent {


    public BottleRedeemEvent(Player player, ItemStack item, double amount, String type, boolean offHand) {
        super(player, item, amount, type, offHand);
    }
}
