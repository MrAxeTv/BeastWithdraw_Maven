package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.lib.xmaterials.XMaterial;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.utils.Utils;
import net.minecraft.world.entity.monster.Drowned;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import javax.swing.plaf.ButtonUI;
import java.util.UUID;

public class DispenserXpBottleListener implements Listener {

    private BeastWithdrawPlugin pl;

    private String id;

    private Material xpBottleMaterial = XMaterial.EXPERIENCE_BOTTLE.parseMaterial();

    public DispenserXpBottleListener(BeastWithdrawPlugin pl) {
        if (MinecraftVersion.isNewerThan(MinecraftVersion.MC1_15_R1)) return;
        this.pl = pl;
        id = pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.NBTKey");
        pl.getServer().getPluginManager().registerEvents(this,pl);

    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onDispense(BlockDispenseEvent e) {
        if (e.getItem().getType() == xpBottleMaterial) {
            NBTItem nbtItem = new NBTItem(e.getItem());
            if (!nbtItem.hasKey(id)) return;
            Location dispenserLocation = e.getBlock().getLocation();
            World world = dispenserLocation.getWorld();
            ThrownExpBottle thrownExpBottle = world.spawn(getBlockFacingDispenser(e.getBlock()).getLocation().add(0.5,0.5,0.5), ThrownExpBottle.class);
           thrownExpBottle.setVelocity(e.getVelocity());
            thrownExpBottle.setCustomName("XPB:" + nbtItem.getInteger(id));
            Dispenser state = (Dispenser) e.getBlock().getState();

            e.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    Inventory inv = state.getInventory();
                    if(inv.containsAtLeast(e.getItem(), 1)) {
                        inv.removeItem(e.getItem());
                    }
                }
            }.runTaskLater(pl,1);
        }
    }
    public static Block getBlockFacingDispenser(Block dispenserBlock) {
        if (dispenserBlock.getType() == Material.DISPENSER) {
            org.bukkit.material.Dispenser dispenserData = (org.bukkit.material.Dispenser) dispenserBlock.getState().getData();
            BlockFace facing = dispenserData.getFacing();
            return dispenserBlock.getRelative(facing);
        }
        return null;
    }
}
