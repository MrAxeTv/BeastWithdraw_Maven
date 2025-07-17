package me.mraxetv.beastwithdraw.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BTokensRedeemEvent extends CustomRedeemEvent implements Cancellable {

    public BTokensRedeemEvent(Player p, ItemStack item, double cash,String type,boolean offHand) {
        super(p,item,cash,type,offHand);
    }


}
