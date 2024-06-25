package me.mraxetv.beastwithdraw.listener;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

import java.util.List;


public class CancelCraftingListener
        implements Listener {
    BeastWithdrawPlugin pl;
    private boolean crafting;
    private boolean hasShopKeeper;

    public CancelCraftingListener(BeastWithdrawPlugin pl) {
        this.pl = pl;
        pl.getServer().getPluginManager().registerEvents(this, pl);
        crafting = pl.getConfig().getBoolean("Settings.CancelCrafting");
        hasShopKeeper = pl.getServer().getPluginManager().isPluginEnabled("Shopkeepers");
    }


    @EventHandler
    public void nocraft(InventoryClickEvent e) {
        if (!crafting) return;
        if (e.getWhoClicked().getOpenInventory().getTopInventory().getType() != InventoryType.WORKBENCH) return;
        ItemStack itemStack = e.getCurrentItem();
        if (e.getCurrentItem() == null) return;
        if (e.getCurrentItem().getType() == Material.AIR) return;
        if (!itemStack.hasItemMeta()) return;
        NBTItem nbtItem = new NBTItem(itemStack);
        if (!nbtItem.hasKey("bCraft")) return;
        e.setCancelled(true);
        String s = pl.getMessages().getString("Withdraws.CancelCrafting");
        pl.getUtils().sendMessage(e.getView().getPlayer(), s);
        return;

    }

    @EventHandler
    public void noGrindStone(InventoryClickEvent e) {
        if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1)) return;
        if (e.getWhoClicked().getOpenInventory().getTopInventory().getType() != InventoryType.GRINDSTONE) return;
        ItemStack itemStack = e.getCurrentItem();
        if (e.getCurrentItem() == null) return;
        if (e.getCurrentItem().getType() == Material.AIR) return;
        if (!itemStack.hasItemMeta()) return;
        NBTItem nbtItem = new NBTItem(itemStack);
        if (!nbtItem.hasKey("bCraft")) return;
        e.setCancelled(true);
        String s = pl.getMessages().getString("Withdraws.CancelGrindStone");
        pl.getUtils().sendMessage(e.getView().getPlayer(), s);
        return;

    }


    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if ((!pl.getConfig().getBoolean("Settings.VillagerTrade.Allow")) &&
                (e.getInventory().getType() == InventoryType.MERCHANT)) {
            Player p = (Player) e.getWhoClicked();
            if (e.getCurrentItem() == null) return;
            if (e.getCurrentItem().getType() == Material.AIR) return;
            ItemStack item = e.getCurrentItem();
            if (!item.hasItemMeta()) return;

            NBTItem tag = new NBTItem(item);


            if (tag.hasKey("bCraft")) {

                if (e.getWhoClicked().getOpenInventory().getTopInventory().getHolder() == null && pl.getConfig().getBoolean("Settings.VillagerTrade.AllowShopKeeperPlugin")) return;

                String s = pl.getMessages().getString("Withdraws.CancelVillagerTrade");
                pl.getUtils().sendMessage(p, s);
                e.setCancelled(true);


            }
        }
    }



}

