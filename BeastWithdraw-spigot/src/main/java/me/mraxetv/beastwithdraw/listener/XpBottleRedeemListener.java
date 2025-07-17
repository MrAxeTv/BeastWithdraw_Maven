package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.events.BottleRedeemEvent;
import me.mraxetv.beastwithdraw.managers.assets.XpBottleHandler;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

import java.util.HashSet;
import java.util.UUID;

public class XpBottleRedeemListener implements Listener {
    private BeastWithdrawPlugin pl;
    private HashSet<UUID> delayList;
    private boolean autoCollect = false;
    private XpBottleHandler assetHandler;

    public XpBottleRedeemListener(BeastWithdrawPlugin plugin, XpBottleHandler assetHandler) {
        pl = plugin;
        this.assetHandler = assetHandler;
        pl.getServer().getPluginManager().registerEvents(this, pl);
        delayList = new HashSet<>();
        autoCollect = assetHandler.getConfig().getBoolean("Settings.AutoCollect");
        if (!autoCollect && assetHandler.getMaterial() != XMaterial.EXPERIENCE_BOTTLE.parseMaterial()) autoCollect = true;

    }

    @EventHandler
    public void redeemEvent(BottleRedeemEvent e) {
        Player p = e.getPlayer();
        if (e.isCancelled()) return;
        ItemStack item = e.getItem();

        int tax = (int)Math.ceil(assetHandler.calculateTax(p, e.getAmount(), e.getItem()));
        int amount = (int)e.getAmount() - tax;
        if (autoCollect) {
            XpManager.setTotalExperience(p, XpManager.getTotalExperience(p) + amount);
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
        } else {
            ThrownExpBottle t = e.getPlayer().launchProjectile(ThrownExpBottle.class);
            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_15_R1)) {
                t.setItem(item);
            } else {
                t.setCustomName("XPB:" + amount);
            }
        }
        if (assetHandler.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            try {
                String sound = assetHandler.getConfig().getString("Settings.Sounds.Redeem.Sound");
                p.playSound(p.getLocation(), Sound.valueOf(sound), 1f, 1f);

            } catch (Exception e1) {
                Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "§cBroken sound in XpBottle Redeem section!");
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

    @EventHandler(priority = EventPriority.LOW)
    public void xpThrow(ExpBottleEvent e) {

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_15_R1)) {
            ThrownExpBottle thrownExpBottle = e.getEntity();
            if (thrownExpBottle == null) return;
            NBTItem nbtItem = new NBTItem(e.getEntity().getItem());
            if (!nbtItem.hasKey(assetHandler.getConfig().getString("Settings.NBTKey"))) return;
            int xp = nbtItem.getInteger(assetHandler.getConfig().getString("Settings.NBTKey"));
            e.setExperience(xp - (int)assetHandler.calculateTax(((Player) e.getEntity().getShooter()),xp,e.getEntity().getItem()));
        } else {
            if (e.getEntity().getCustomName() == null) return;
            if (!e.getEntity().getCustomName().startsWith("XPB:")) return;
            int xp = Integer.parseInt(e.getEntity().getCustomName().replaceAll("XPB:", ""));
            e.setExperience(xp);

        }
    }


    @EventHandler
    public void playerDeath(PlayerDeathEvent e) {
        if (!assetHandler.getConfig().getBoolean("Settings.DropOnDeath")) return;
        Player p = e.getEntity();

        if (!p.hasPermission("BeastWithdraw.XpBottle.Drop")) return;
        int xp = XpManager.getTotalExperience(p);
        if (xp <= 0) return;
        double dropPercentage = assetHandler.getConfig().getDouble("Settings.DropPercentage") / 100;
        xp = (int) (xp * dropPercentage);

        ItemStack xpbItemStack = assetHandler.getItem(p.getName(), xp, 1, true, assetHandler.getConfig().getDouble("Settings.Charges.Tax.Percentage"));

        p.getWorld().dropItem(p.getLocation(), xpbItemStack);
        p.setTotalExperience(0);
        p.setLevel(0);
        p.setExp(0);
        e.setDroppedExp(0);
    }
}