package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;

import me.mraxetv.beasttokens.BeastTokensPlugin;
import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.events.BTokensRedeemEvent;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;


public class BTokensNoteRedeemListener implements Listener {
    private BeastWithdrawPlugin pl;
    private HashSet<UUID> delayList;

    public BTokensNoteRedeemListener(BeastWithdrawPlugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("BeastTokens")) return;
        pl = plugin;
        pl.getServer().getPluginManager().registerEvents(this,pl);
        delayList = new HashSet<>();
        if(pl.getServer().getPluginManager().getPlugin("BeastTokens") != null) BeastTokensPlugin.getInstance().getDeposit().disable();
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
        if (!nbtItem.hasKey(pl.getWithdrawManager().BEASTTOKENS_NOTE.getConfig().getString("Settings.NBTLore"))) return;

        UUID uuid = e.getPlayer().getUniqueId();
        if(delayList.contains(uuid)) return;
        delayList.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                delayList.remove(uuid);
            }
        }.runTaskLater(pl,1);

        e.setCancelled(true);

        boolean offHand = false;

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1)) {
            if (item.equals(e.getPlayer().getInventory().getItemInOffHand())) {
                offHand = true;
            }
        }
        pl.getServer().getPluginManager().
                callEvent(new BTokensRedeemEvent(e.getPlayer(), item, nbtItem.getDouble(pl.getWithdrawManager().BEASTTOKENS_NOTE.getConfig().getString("Settings.NBTLore")), offHand));
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void redeemEvent(BTokensRedeemEvent e) {

        if (e.isCancelled()) return;
        ItemStack item = e.getItem();
        Player p = e.getPlayer();
        double tokens = e.getTokens();
        BeastTokensAPI.getTokensManager().addTokens(p,tokens);
        String msg = pl.getMessages().getString("Withdraws.BeastTokensNote.Redeem");
        msg = msg.replaceAll("%received-amount%", "" + pl.getUtils().formatDouble(tokens));
        msg = msg.replaceAll("%balance%", "" + pl.getUtils().formatDouble(BeastTokensAPI.getTokensManager().getTokens(p)));

        pl.getUtils().sendMessage(p,msg);

        if (pl.getWithdrawManager().BEASTTOKENS_NOTE.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            try {
                String sound = pl.getWithdrawManager().BEASTTOKENS_NOTE.getConfig().getString("Settings.Sounds.Redeem.Sound");
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.valueOf(sound), 1f, 1f);

            } catch (Exception e1) {
                Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "�cBroken sound in BeastTokensNote Redeem section!");
            }
        }


        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else if (e.inOffHand()) {
            p.getInventory().setItemInOffHand(null);
        } else {
            p.getInventory().removeItem(new ItemStack[]{item});
        }
        p.updateInventory();

    }


}
