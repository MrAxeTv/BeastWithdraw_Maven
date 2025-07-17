package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.events.BTokensRedeemEvent;
import me.mraxetv.beastwithdraw.events.CashRedeemEvent;
import me.mraxetv.beastwithdraw.events.CustomRedeemEvent;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;


public class RedeemListener implements Listener {
    private BeastWithdrawPlugin pl;
    private HashSet<UUID> delayList;

    public RedeemListener(BeastWithdrawPlugin plugin) {
        pl = plugin;
        pl.getServer().getPluginManager().registerEvents(this, pl);
        delayList = new HashSet<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void mainHand(PlayerInteractEvent e) {
        if (!e.hasItem()) return;
        if (e.getItem().getType() == Material.AIR) return;
        ItemStack item = e.getItem();
        if (!item.hasItemMeta()) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        //if (item.getType() != material) return;

        NBTItem nbtItem = new NBTItem(item);
        if (!nbtItem.hasKey("RedeemType")) return;
        String type = nbtItem.getString("RedeemType").toLowerCase();

        AssetHandler assetHandler = pl.getWithdrawManager().getAssetHandler(type);
        double amount = nbtItem.getDouble(assetHandler.getNbtTag());
        UUID uuid = e.getPlayer().getUniqueId();
        if (delayList.contains(uuid)) return;
        delayList.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                delayList.remove(uuid);
            }
        }.runTaskLater(pl, 1);
        e.setCancelled(true);
        boolean offHand = false;

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1)) {
            if (item.equals(e.getPlayer().getInventory().getItemInOffHand())) {
                offHand = true;
            }
        }
        CustomRedeemEvent event = RedeemRegistry.createEvent(type, e.getPlayer(), item, amount, offHand);
        Bukkit.getPluginManager().callEvent(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void redeemEvent(BTokensRedeemEvent e) {
        handleGenericRedeem(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void redeemEvent(CashRedeemEvent e) {
        handleGenericRedeem(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void redeemEvent(CustomRedeemEvent e) {
        if (!e.getClass().equals(CustomRedeemEvent.class)) return;
        handleGenericRedeem(e);
    }

    private void handleGenericRedeem(CustomRedeemEvent e) {
        if (e.isCancelled()) return;
        ItemStack item = e.getItem();
        Player p = e.getPlayer();
        AssetHandler assetHandler = pl.getWithdrawManager().getAssetHandler(e.getType());
        double tax = assetHandler.calculateTax(p, e.getAmount(), e.getItem());
        double amount = e.getAmount() - tax;
        assetHandler.depositAmount(p, amount);
        String msg;
        if (tax == 0) {
            msg = assetHandler.getMessageSection().getString("Redeem");
        } else {
            msg = assetHandler.getMessageSection().getString("RedeemAndTax");
        }
        msg = msg.replace("%amount%", "" + assetHandler.formatWithPreSuffix(amount));
        msg = msg.replace("%balance%", "" + assetHandler.formatWithPreSuffix(assetHandler.getBalance(p)));
        msg = msg.replace("%tax%", "" + assetHandler.formatWithPreSuffix(tax));
        pl.getUtils().sendMessage(p, msg);
        if (assetHandler.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            try {
                String sound = assetHandler.getConfig().getString("Settings.Sounds.Redeem.Sound");
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.valueOf(sound), 1f, 1f);
            } catch (Exception e1) {
                Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "�cBroken sound in BeastTokensNote Redeem section!");
            }
        }
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else if (e.isOffHand()) {
            p.getInventory().setItemInOffHand(null);
        } else {
            p.getInventory().removeItem(new ItemStack[]{item});
        }
        p.updateInventory();
    }
}
