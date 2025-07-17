package me.mraxetv.beastwithdraw.managers.redeem;

import me.mraxetv.beastwithdraw.events.CustomRedeemEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class RedeemRegistry {
    private static final Map<String, RedeemEventFactory> factoryMap = new HashMap<>();

    public static void register(String type, RedeemEventFactory factory) {
        factoryMap.put(type.toLowerCase(), factory);
    }

    public static CustomRedeemEvent createEvent(String type, Player player, ItemStack item, double amount,boolean offHand) {
        RedeemEventFactory factory = factoryMap.getOrDefault(type.toLowerCase(), (p, i, a,t, o) -> new CustomRedeemEvent(p, i, a, t, o));
        return factory.create(player, item, amount, type,offHand);
    }

    public static boolean isRegistered(String type) {
        return factoryMap.containsKey(type.toLowerCase());
    }
}

