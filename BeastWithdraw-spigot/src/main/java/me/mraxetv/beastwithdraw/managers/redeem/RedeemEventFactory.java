package me.mraxetv.beastwithdraw.managers.redeem;

import me.mraxetv.beastwithdraw.events.CustomRedeemEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface RedeemEventFactory {
    CustomRedeemEvent create(Player player, ItemStack item, double amount,String type, boolean offHand);
}
