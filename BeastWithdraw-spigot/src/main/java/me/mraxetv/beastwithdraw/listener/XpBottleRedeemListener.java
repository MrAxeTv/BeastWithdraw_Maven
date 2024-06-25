package me.mraxetv.beastwithdraw.listener;


import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.events.BottleRedeemEvent;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;


public class XpBottleRedeemListener implements Listener {
    private BeastWithdrawPlugin pl;
    private HashSet<UUID> delayList;
    private boolean autoCollect = false;
    public XpBottleRedeemListener(BeastWithdrawPlugin plugin) {
        pl = plugin;
        pl.getServer().getPluginManager().registerEvents(this, pl);
        delayList = new HashSet<>();
        autoCollect = pl.getWithdrawManager().XP_BOTTLE.getConfig().getBoolean("Settings.AutoCollect");
        if(!autoCollect && pl.getWithdrawManager().XP_BOTTLE.getMaterial() != XMaterial.EXPERIENCE_BOTTLE.parseMaterial()) autoCollect = true;

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void mainHand(PlayerInteractEvent e) {
        if (!e.hasItem()) return;
        if (e.getItem().getType() == Material.AIR) return;
        if (!e.getItem().hasItemMeta()) return;
        ItemStack item = e.getItem();
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        NBTItem nbtItem = new NBTItem(item);
        if (!nbtItem.hasKey(pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.NBTLore"))) return;

        UUID uuid = e.getPlayer().getUniqueId();
        if(delayList.contains(uuid)) return;
        delayList.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {delayList.remove(uuid);}
        }.runTaskLater(pl,1);

        e.setCancelled(true);

        boolean offHand = false;

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1)) {
            if (item.equals(e.getPlayer().getInventory().getItemInOffHand())) {
                offHand = true;
            }
        }

        pl.getServer().getPluginManager().
                callEvent(new BottleRedeemEvent(e.getPlayer(), item, nbtItem.getInteger(pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.NBTLore")), offHand));


    }

    @EventHandler
    public void redeemEvent(BottleRedeemEvent e) {
        Player p = e.getPlayer();

        if (e.isCancelled()) return;
        ItemStack item = e.getItem();
        if (autoCollect) {

            int xp = XpManager.getTotalExperience(p);
            int receivedXp = e.getExp();

            XpManager.setTotalExperience(p, xp + receivedXp);

            String s = pl.getMessages().getString("Withdraws.XpBottle.Redeem");
            s = s.replaceAll("%received-amount%", "" + pl.getUtils().formatNumber(receivedXp));
            s = s.replaceAll("%balance%", "" + pl.getUtils().formatNumber(XpManager.getTotalExperience(p)));
            pl.getUtils().sendMessage(p, s);
        } else {
            ThrownExpBottle t = e.getPlayer().launchProjectile(ThrownExpBottle.class);
            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_15_R1)) {
                t.setItem(item);
            } else {
                t.setCustomName("XPB:" + e.getExp());
            }
        }
        if (pl.getWithdrawManager().XP_BOTTLE.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            try {
                String sound = pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.Sounds.Redeem.Sound");
                p.playSound(p.getLocation(), Sound.valueOf(sound), 1f, 1f);

            } catch (Exception e1) {
                Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "§cBroken sound in XpBottle Redeem section!");
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


    @EventHandler(priority = EventPriority.LOWEST)
    public void xpThrow(ExpBottleEvent e) {

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_15_R1)) {
            ThrownExpBottle thrownExpBottle = e.getEntity();
            if (thrownExpBottle == null) return;
            NBTItem nbtItem = new NBTItem(e.getEntity().getItem());
            if (!nbtItem.hasKey(pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.NBTLore"))) return;
            e.setExperience(nbtItem.getInteger(pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.NBTLore")));
        } else {
            if (e.getEntity().getCustomName() == null) return;
            if (!e.getEntity().getCustomName().startsWith("XPB:")) return;
            int xp = Integer.parseInt(e.getEntity().getCustomName().replaceAll("XPB:", ""));
            e.setExperience(xp);

        }
    }


    @EventHandler
    public void PlayerDeath(PlayerDeathEvent e) {
        if (!pl.getWithdrawManager().XP_BOTTLE.getConfig().getBoolean("Settings.DropOnDeath")) return;
        Player p = e.getEntity();

        if (!p.hasPermission("BeastWithdraw.XpBottle.Drop")) return;
        int xp = XpManager.getTotalExperience(p);
        if (xp <= 0) return;
        double dropPercentage = pl.getWithdrawManager().XP_BOTTLE.getConfig().getDouble("Settings.DropPercentage") / 100;
        xp = (int) (xp * dropPercentage);

        ItemStack Xpb = pl.getWithdrawManager().XP_BOTTLE.getItem(p.getName(), xp, 1, true);

        p.getWorld().dropItem(p.getLocation(), Xpb);
        p.setTotalExperience(0);
        p.setLevel(0);
        p.setExp(0);
        e.setDroppedExp(0);
    }

}
