package me.mraxetv.beastwithdraw.listener;



import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.events.CashRedeemEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;


public class CashNoteRedeemListener implements Listener {
    private BeastWithdrawPlugin pl;
    private HashSet<UUID> delayList;


    public CashNoteRedeemListener(BeastWithdrawPlugin plugin) {
        pl = plugin;
        pl.getServer().getPluginManager().registerEvents(this,pl);
        delayList = new HashSet<>();
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void mainHand(PlayerInteractEvent e) {

        if ((!e.hasItem()) ) return;
        if (e.getItem().getType() == Material.AIR) return;
        if (!e.getItem().hasItemMeta()) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        NBTItem nbtItem = new NBTItem(e.getItem());
        if (!nbtItem.hasKey(pl.getWithdrawManager().CASH_NOTE.getConfig().getString("Settings.NBTLore"))) return;
        UUID uuid = e.getPlayer().getUniqueId();
        if(delayList.contains(uuid)) return;
        delayList.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                delayList.remove(uuid);
            }
        }.runTaskLater(pl,1);
        //Cancel First Time
        e.setCancelled(true);

        boolean offHand = false;

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1)) {
            if (e.getItem().equals(e.getPlayer().getInventory().getItemInOffHand())) {
                offHand = true;
            }
        }
        pl.getServer().getPluginManager().
                callEvent(new CashRedeemEvent(e.getPlayer(), e.getItem(), nbtItem.getDouble(pl.getWithdrawManager().CASH_NOTE.getConfig().getString("Settings.NBTLore")), offHand));

    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void redeemEvent(CashRedeemEvent e) {

        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        double cash = e.getCash();
        pl.getEcon().depositPlayer(p, cash);
        String msg = pl.getMessages().getString("Withdraws.CashNote.Redeem");
        msg = msg.replaceAll("%received-amount%", "" + pl.getUtils().formatDouble(cash));
        msg = msg.replaceAll("%balance%", "" + pl.getUtils().formatDouble(pl.getEcon().getBalance(e.getPlayer())));

        pl.getUtils().sendMessage(p,msg);

        if (pl.getWithdrawManager().CASH_NOTE.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            try {
                String sound = pl.getWithdrawManager().CASH_NOTE.getConfig().getString("Settings.Sounds.Redeem.Sound");
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.valueOf(sound), 1f, 1f);

            } catch (Exception e1) {
                Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "�cBroken sound in CashNote Redeem section!");
            }
        }


        if (e.getItem().getAmount() > 1) {
            e.getItem().setAmount(e.getItem().getAmount() - 1);
        } else if (e.inOffHand()) {
            p.getInventory().setItemInOffHand(null);
        } else {
            p.getInventory().removeItem(new ItemStack[]{e.getItem()});
        }
        p.updateInventory();

    }


}
