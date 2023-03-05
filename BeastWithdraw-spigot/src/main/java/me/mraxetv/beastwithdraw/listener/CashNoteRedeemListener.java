package me.mraxetv.beastwithdraw.listener;

import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.events.CashRedeemEvent;
import me.mraxetv.beastwithdraw.utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

import net.milkbowl.vault.economy.Economy;


public class CashNoteRedeemListener implements Listener {
    private BeastWithdrawPlugin pl;
    private Material material;

    public CashNoteRedeemListener(BeastWithdrawPlugin plugin) {
        pl = plugin;
        material = Material.getMaterial(pl.getWithdrawManager().getCashNoteConfig().getString("Settings.Item"));
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void mainHand(PlayerInteractEvent e) {

        if ((!e.hasItem()) ) return;
        if (e.getItem().getType() == Material.AIR) return;
        if (!e.getItem().hasItemMeta()) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        //if (e.getItem().getType() != material) return;
        NBTItem nbtItem = new NBTItem(e.getItem());
        if (!nbtItem.hasKey(pl.getWithdrawManager().getCashNoteConfig().getString("Settings.NBTLore"))) return;

        //Cancel dupe event on block click
        if (e.isCancelled() && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            return;
        }
        //Cancel First Time
        e.setCancelled(true);

        boolean offHand = false;

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1)) {
            if (e.getItem().equals(e.getPlayer().getInventory().getItemInOffHand())) {
                offHand = true;
            }
        }
        pl.getServer().getPluginManager().
                callEvent(new CashRedeemEvent(e.getPlayer(), e.getItem(), nbtItem.getDouble(pl.getWithdrawManager().getCashNoteConfig().getString("Settings.NBTLore")), offHand));
        return;

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

        if (pl.getWithdrawManager().getCashNoteConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            try {
                String sound = pl.getWithdrawManager().getCashNoteConfig().getString("Settings.Sounds.Redeem.Sound");
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
