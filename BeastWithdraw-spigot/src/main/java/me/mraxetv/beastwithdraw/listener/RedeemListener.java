package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.events.BTokensRedeemEvent;
import me.mraxetv.beastwithdraw.events.CashRedeemEvent;
import me.mraxetv.beastwithdraw.events.CustomRedeemEvent;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.assets.CashNoteHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import me.mraxetv.beastwithdraw.utils.MessagesLang;
import me.mraxetv.beastwithdraw.utils.Utils;
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
import java.math.BigDecimal;


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
        if(e.getClickedBlock() != null && e.getClickedBlock().getType().toString().toLowerCase().contains("shelf")) return;

        String type = nbtItem.getString("RedeemType").toLowerCase();
        AssetHandler assetHandler = pl.getWithdrawManager().getAssetHandler(type);
        double amount = assetHandler instanceof CashNoteHandler
                ? ((CashNoteHandler) assetHandler).getStoredAmount(item).doubleValue()
                : nbtItem.getDouble(assetHandler.getNbtTag());
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

        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        int stackSize = 1;

        AssetHandler assetHandler = pl.getWithdrawManager().getAssetHandler(e.getType());
        if (assetHandler instanceof CashNoteHandler) {
            handleCashRedeem(e, (CashNoteHandler) assetHandler);
            return;
        }

        if(!p.hasPermission("BeastWithdraw." + assetHandler.getID() + ".Redeem")){
            pl.getUtils().noPermission(p);
            return;
        }
        boolean fullStack = false;

        // Sneaking + stack permission check
        if (item.getAmount() > 1 && p.isSneaking() &&
                (p.hasPermission("BeastWithdraw." + assetHandler.getID() + ".Redeem.Stacked") )) {

            stackSize = item.getAmount();
            fullStack = true;
        }

        double singleAmount = e.getAmount();
        double singleTax = Math.ceil(assetHandler.calculateTax(singleAmount, item));
        double singleAfterTax = singleAmount - singleTax;

        double totalAmount = singleAmount * stackSize;
        double totalTax = singleTax * stackSize;
        double finalAmount = totalAmount - totalTax;

        assetHandler.depositAmount(p, finalAmount);

        // Message logic
        String msg;
        if (totalTax == 0) {
            msg = assetHandler.getMessageSection().getString("Redeem");
        } else {
            msg = assetHandler.getMessageSection().getString("RedeemAndTax");
        }

        msg = msg.replace("%amount%", assetHandler.formatWithPreSuffix(singleAfterTax));
        msg = msg.replace("%tax%", assetHandler.formatWithPreSuffix(singleTax));
        msg = msg.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalanceAsDouble(p)));
        msg = Utils.formatStackSize(msg,stackSize);

        msg = msg.replace("%stacked-amount%", assetHandler.formatWithPreSuffix(finalAmount));
        msg = msg.replace("%stacked-tax%", assetHandler.formatWithPreSuffix(totalTax));
        pl.getUtils().sendMessage(p, msg);

        // Item removal
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
                p.getInventory().removeItem(item);
            }
        }

        p.updateInventory();

        // Sound handling
        if (assetHandler.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            String soundName = assetHandler.getConfig().getString("Settings.Sounds.Redeem.Sound");
            float volume = assetHandler.getConfig().getDouble("Settings.Sounds.Redeem.Volume", 1.0).floatValue();
            float pitch = assetHandler.getConfig().getDouble("Settings.Sounds.Redeem.Pitch", 1.0).floatValue();

            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                p.playSound(p.getLocation(), sound, volume, pitch);
            } catch (Exception ex) {
                Bukkit.getServer().getConsoleSender().sendMessage(
                        pl.getUtils().getPrefix() + "§cBroken sound in BeastWithdraw redeem section!");
            }
        }

        pl.getWithdrawLogger().logRedeem(assetHandler, p, singleAmount, stackSize, totalAmount, totalTax, finalAmount, assetHandler.getBalanceAsDouble(p));
    }

    private void handleCashRedeem(CustomRedeemEvent e, CashNoteHandler assetHandler) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        int stackSize = 1;

        if (!player.hasPermission("BeastWithdraw." + assetHandler.getID() + ".Redeem")) {
            pl.getUtils().noPermission(player);
            return;
        }

        boolean fullStack = false;
        if (item.getAmount() > 1 && player.isSneaking()
                && player.hasPermission("BeastWithdraw." + assetHandler.getID() + ".Redeem.Stacked")) {
            stackSize = item.getAmount();
            fullStack = true;
        }

        BigDecimal singleAmount = assetHandler.getStoredAmount(item);
        BigDecimal singleTax = assetHandler.calculateRedeemTax(singleAmount, item);
        BigDecimal singleAfterTax = singleAmount.subtract(singleTax);

        BigDecimal totalAmount = singleAmount.multiply(BigDecimal.valueOf(stackSize));
        BigDecimal totalTax = singleTax.multiply(BigDecimal.valueOf(stackSize));
        BigDecimal finalAmount = totalAmount.subtract(totalTax);

        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            pl.getUtils().sendMessage(player, MessagesLang.TRANSACTION_FAILED);
            return;
        }

        CashNoteHandler.CashTransactionResult transaction = assetHandler.redeemNote(player, finalAmount);
        if (!transaction.isSuccess()) {
            pl.getUtils().sendMessage(player, MessagesLang.TRANSACTION_FAILED);
            return;
        }

        String message = totalTax.compareTo(BigDecimal.ZERO) == 0
                ? assetHandler.getMessageSection().getString("Redeem")
                : assetHandler.getMessageSection().getString("RedeemAndTax");

        message = message.replace("%amount%", assetHandler.formatWithPreSuffix(singleAfterTax.doubleValue()));
        message = message.replace("%tax%", assetHandler.formatWithPreSuffix(singleTax.doubleValue()));
        message = message.replace("%balance%", assetHandler.formatWithPreSuffix(transaction.getBalanceAfter().doubleValue()));
        message = Utils.formatStackSize(message, stackSize);
        message = message.replace("%stacked-amount%", assetHandler.formatWithPreSuffix(finalAmount.doubleValue()));
        message = message.replace("%stacked-tax%", assetHandler.formatWithPreSuffix(totalTax.doubleValue()));
        pl.getUtils().sendMessage(player, message);

        if (fullStack) {
            if (e.isOffHand()) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().removeItem(item);
            }
        } else {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else if (e.isOffHand()) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().removeItem(item);
            }
        }

        player.updateInventory();

        if (assetHandler.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            String soundName = assetHandler.getConfig().getString("Settings.Sounds.Redeem.Sound");
            float volume = assetHandler.getConfig().getDouble("Settings.Sounds.Redeem.Volume", 1.0).floatValue();
            float pitch = assetHandler.getConfig().getDouble("Settings.Sounds.Redeem.Pitch", 1.0).floatValue();

            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception ex) {
                Bukkit.getServer().getConsoleSender().sendMessage(
                        pl.getUtils().getPrefix() + "Â§cBroken sound in BeastWithdraw redeem section!");
            }
        }

        pl.getWithdrawLogger().logRedeem(
                assetHandler,
                player,
                singleAmount.doubleValue(),
                stackSize,
                totalAmount.doubleValue(),
                totalTax.doubleValue(),
                finalAmount.doubleValue(),
                transaction.getBalanceAfter().doubleValue()
        );
    }


}
