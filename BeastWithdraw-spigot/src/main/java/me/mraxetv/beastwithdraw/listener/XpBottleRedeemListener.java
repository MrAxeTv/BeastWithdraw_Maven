package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.events.BottleRedeemEvent;
import me.mraxetv.beastwithdraw.managers.assets.XpBottleHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
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
import org.bukkit.projectiles.BlockProjectileSource;

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

        int stackSize = 1;
        boolean fullStack = false;

        if (!p.hasPermission("BeastWithdraw." + assetHandler.getID() + ".Redeem")) {
            pl.getUtils().noPermission(p);
            return;
        }

        // Check for stacked redeem
        if (item.getAmount() > 1 && p.isSneaking() &&
                p.hasPermission("BeastWithdraw." + assetHandler.getID() + ".Redeem.Stacked")) {
            stackSize = item.getAmount();
            fullStack = true;
        }

        int singleAmount = (int) e.getAmount();
        int singleTax = (int) Math.ceil(assetHandler.calculateTax(singleAmount, item));
        int singleAfterTax = singleAmount - singleTax;

        int totalAmount = singleAmount * stackSize;
        int totalTax = singleTax * stackSize;
        int finalAmount = totalAmount - totalTax;

        if (autoCollect) {
            XpManager.setTotalExperience(p, XpManager.getTotalExperience(p) + finalAmount);

            String msg;
            if (totalTax == 0) {
                msg = assetHandler.getMessageSection().getString("Redeem");
            } else {
                msg = assetHandler.getMessageSection().getString("RedeemAndTax");
            }

            msg = msg.replace("%amount%", assetHandler.formatWithPreSuffix(singleAfterTax));
            msg = msg.replace("%tax%", assetHandler.formatWithPreSuffix(singleTax));
            msg = msg.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalance(p)));
            msg = msg.replace("%level-amount%", assetHandler.formatNumber(XpManager.getLevelFromExp(singleAfterTax)));
            msg = msg.replace("%level-tax%", assetHandler.formatNumber(XpManager.getLevelFromExp(singleTax)));
            msg = Utils.formatStackSize(msg,stackSize);

            msg = msg.replace("%stacked-amount%", assetHandler.formatWithPreSuffix(finalAmount));
            msg = msg.replace("%stacked-tax%", assetHandler.formatWithPreSuffix(totalTax));


            pl.getUtils().sendMessage(p, msg);
        } else {
            // Launch a single bottle for the total XP
            ThrownExpBottle t = p.launchProjectile(ThrownExpBottle.class);
            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_15_R1)) {
                NBTItem nbtItem = new NBTItem(item);
                nbtItem.setInteger(assetHandler.getConfig().getString("Settings.NBTKey"), finalAmount);
                t.setItem(nbtItem.getItem());
            } else {
                t.setCustomName("XPB:" + finalAmount);
            }
        }

        // Play redeem sound
        if (assetHandler.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            String soundName = assetHandler.getConfig().getString("Settings.Sounds.Redeem.Sound");
            float volume = assetHandler.getConfig().getDouble("Settings.Sounds.Redeem.Volume", 1.0).floatValue();
            float pitch = assetHandler.getConfig().getDouble("Settings.Sounds.Redeem.Pitch", 1.0).floatValue();

            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                p.playSound(p.getLocation(), sound, volume, pitch);
            } catch (Exception e1) {
                Bukkit.getServer().getConsoleSender().sendMessage(
                        pl.getUtils().getPrefix() + "§cBroken sound in XpBottle Redeem section!");
            }
        }

        // Remove the stack or just 1 item
        if (fullStack) {
            if (e.isOffHand()) {
                p.getInventory().setItemInOffHand(null);
            } else {
                p.getInventory().removeItem(item);
            }
        } else {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else if (e.isOffHand()) {
                p.getInventory().setItemInOffHand(null);
            } else {
                p.getInventory().removeItem(new ItemStack[]{item});
            }
        }

        p.updateInventory();

        pl.getWithdrawLogger().logRedeem(assetHandler, p, singleAmount, stackSize, totalAmount, totalTax, finalAmount, assetHandler.getBalance(p));
    }



    @EventHandler(priority = EventPriority.LOW)
    public void xpThrow(ExpBottleEvent e) {

        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_15_R1)) {
            ThrownExpBottle thrownExpBottle = e.getEntity();
            if (thrownExpBottle == null) return;
            NBTItem nbtItem = new NBTItem(e.getEntity().getItem());
            if (!nbtItem.hasKey(assetHandler.getConfig().getString("Settings.NBTKey"))) return;
            int xp = nbtItem.getInteger(assetHandler.getConfig().getString("Settings.NBTKey"));
            // Apply tax if dispensed from a dispenser
            if(e.getEntity().getShooter() instanceof BlockProjectileSource){
                int singleTax = (int) Math.ceil(pl.getWithdrawManager().XP_BOTTLE.calculateTax(xp, e.getEntity().getItem()));
                xp = xp-singleTax;
            }
            e.setExperience(xp);
        } else {
            if (e.getEntity().getCustomName() == null) return;
            if (!e.getEntity().getCustomName().startsWith("XPB:")) return;
            int xp = Integer.parseInt(e.getEntity().getCustomName().replace("XPB:", ""));
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
