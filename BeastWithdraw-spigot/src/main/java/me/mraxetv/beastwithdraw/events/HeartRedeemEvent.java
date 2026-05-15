package me.mraxetv.beastwithdraw.events;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HeartRedeemEvent extends CustomRedeemEvent {

    public HeartRedeemEvent(Player player, ItemStack item, double amount, String type, boolean offHand) {
        super(player, item, amount, type, offHand);
    }
}
