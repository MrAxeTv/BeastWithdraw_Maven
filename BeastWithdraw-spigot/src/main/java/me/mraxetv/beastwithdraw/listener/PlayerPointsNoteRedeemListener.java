package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;

import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.events.BTokensRedeemEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;


public class PlayerPointsNoteRedeemListener implements Listener {
    private BeastWithdrawPlugin pl;
    private String handlerID;
    public PlayerPointsNoteRedeemListener(BeastWithdrawPlugin plugin, String handlerID) {
        pl = plugin;
        this.handlerID = handlerID;
        pl.getServer().getPluginManager().registerEvents(this,pl);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void mainHand(PlayerInteractEvent e) {

        if (!e.hasItem()) return;
        if (!e.getItem().hasItemMeta()) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        //if (e.getItem().getType() != material) return;
        NBTItem nbtItem = new NBTItem(e.getItem());
        if (!nbtItem.hasKey(pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getString("Settings.NBTLore"))) return;

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
                callEvent(new BTokensRedeemEvent(e.getPlayer(), e.getItem(), nbtItem.getDouble(pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getString("Settings.NBTLore")), offHand));
        return;

    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void redeemEvent(BTokensRedeemEvent e) {

        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        double tokens = e.getTokens();
        BeastTokensAPI.getTokensManager().addTokens(p,tokens);
        String msg = pl.getMessages().getString("Withdraws.BeastTokensNote.Redeem");
        msg = msg.replaceAll("%received-amount%", "" + pl.getUtils().formatDouble(tokens));
        msg = msg.replaceAll("%balance%", "" + pl.getUtils().formatDouble(BeastTokensAPI.getTokensManager().getTokens(p)));

        pl.getUtils().sendMessage(p,msg);

        if (pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            try {
                String sound = pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getString("Settings.Sounds.Redeem.Sound");
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.valueOf(sound), 1f, 1f);

            } catch (Exception e1) {
                Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "�cBroken sound in BeastTokensNote Redeem section!");
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
