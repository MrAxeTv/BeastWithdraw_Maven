package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.events.HeartRedeemEvent;
import me.mraxetv.beastwithdraw.managers.assets.BeastLifeStealHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class HeartRedeemListener implements Listener {

    private final BeastWithdrawPlugin plugin;
    private final BeastLifeStealHandler assetHandler;

    public HeartRedeemListener(BeastWithdrawPlugin plugin, BeastLifeStealHandler assetHandler) {
        this.plugin = plugin;
        this.assetHandler = assetHandler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void redeemEvent(HeartRedeemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        int stackSize = 1;
        boolean fullStack = false;

        if (!player.hasPermission("BeastWithdraw." + assetHandler.getID() + ".Redeem")) {
            plugin.getUtils().noPermission(player);
            return;
        }

        if (item.getAmount() > 1 && player.isSneaking()
                && player.hasPermission("BeastWithdraw." + assetHandler.getID() + ".Redeem.Stacked")) {
            stackSize = item.getAmount();
            fullStack = true;
        }

        int redeemableCapacity = assetHandler.getRedeemableCapacity(player);
        if (redeemableCapacity <= 0) {
            return;
        }

        int singleAmount = (int) Math.floor(event.getAmount());
        int singleTax = (int) Math.ceil(assetHandler.calculateTax(singleAmount, item));
        int singleAfterTax = Math.max(0, singleAmount - singleTax);
        if (singleAfterTax <= 0) {
            return;
        }

        RedemptionResult result = redeemHearts(player, item, stackSize, redeemableCapacity, event.isOffHand(),
                singleAmount, singleTax, singleAfterTax, fullStack);
        if (result.redeemedAmount <= 0) {
            return;
        }

        assetHandler.depositAmount(player, (double) result.redeemedAmount);

        String message;
        if (result.totalTax == 0) {
            message = assetHandler.getMessageSection().getString("Redeem");
        } else {
            message = assetHandler.getMessageSection().getString("RedeemAndTax");
        }

        message = message.replace("%amount%", assetHandler.formatWithPreSuffix(result.displayAmount));
        message = message.replace("%tax%", assetHandler.formatWithPreSuffix(result.displayTax));
        message = message.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalance(player)));
        message = Utils.formatStackSize(message, result.messageStackSize);
        message = message.replace("%stacked-amount%", assetHandler.formatWithPreSuffix(result.redeemedAmount));
        message = message.replace("%stacked-tax%", assetHandler.formatWithPreSuffix(result.totalTax));
        plugin.getUtils().sendMessage(player, message);

        player.updateInventory();

        plugin.getWithdrawLogger().logRedeem(
                assetHandler,
                player,
                result.displayAmount,
                result.loggedStackSize,
                result.redeemedAmount + result.totalTax,
                result.totalTax,
                result.redeemedAmount,
                assetHandler.getBalance(player)
        );

        assetHandler.playConfiguredSound(player, "Redeem", singleAmount, "Heart Withdraw redeem", assetHandler.getAmountOverrideId(item));
        if (false && assetHandler.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
            String soundName = assetHandler.getConfig().getString("Settings.Sounds.Redeem.Sound");
            float volume = assetHandler.getConfig().getDouble("Settings.Sounds.Redeem.Volume", 1.0).floatValue();
            float pitch = assetHandler.getConfig().getDouble("Settings.Sounds.Redeem.Pitch", 1.0).floatValue();

            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception ex) {
                Bukkit.getServer().getConsoleSender().sendMessage(
                        plugin.getUtils().getPrefix() + "\u00A7cBroken sound in Heart Withdraw redeem section!");
            }
        }
    }

    private RedemptionResult redeemHearts(Player player, ItemStack item, int stackSize, int redeemableCapacity, boolean offHand,
                                          int singleAmount, int singleTax, int singleAfterTax, boolean fullStack) {
        if (singleTax > 0) {
            int fullItemsToRedeem = Math.min(stackSize, redeemableCapacity / singleAfterTax);
            if (fullItemsToRedeem <= 0) {
                return RedemptionResult.none();
            }

            consumeItems(player, item, fullItemsToRedeem, offHand);
            return new RedemptionResult(
                    fullItemsToRedeem * singleAfterTax,
                    fullItemsToRedeem * singleTax,
                    singleAfterTax,
                    singleTax,
                    fullItemsToRedeem,
                    fullItemsToRedeem > 1 ? fullItemsToRedeem : 1
            );
        }

        int totalRedeemable = Math.min(redeemableCapacity, singleAfterTax * stackSize);
        if (totalRedeemable <= 0) {
            return RedemptionResult.none();
        }

        int fullItemsToRedeem = totalRedeemable / singleAfterTax;
        int partialRedeem = totalRedeemable % singleAfterTax;
        if (partialRedeem == 0) {
            consumeItems(player, item, fullItemsToRedeem, offHand);
            return new RedemptionResult(
                    totalRedeemable,
                    0,
                    singleAfterTax,
                    0,
                    fullItemsToRedeem,
                    fullItemsToRedeem > 1 ? fullItemsToRedeem : 1
            );
        }

        double remainingAmount = Math.max(0, singleAmount - partialRedeem);
        ItemStack partialItem = createPartialItem(item, remainingAmount);
        int remainingFullItems = Math.max(0, stackSize - fullItemsToRedeem - 1);

        setHeldItem(player, offHand, partialItem);
        if (remainingFullItems > 0 && fullStack) {
            ItemStack remainingStack = item.clone();
            remainingStack.setAmount(remainingFullItems);
            giveOrDrop(player, remainingStack);
        }

        return new RedemptionResult(
                totalRedeemable,
                0,
                partialRedeem,
                0,
                fullItemsToRedeem + 1,
                1
        );
    }

    private void consumeItems(Player player, ItemStack item, int consumedAmount, boolean offHand) {
        int remaining = item.getAmount() - consumedAmount;
        if (remaining > 0) {
            item.setAmount(remaining);
            return;
        }

        setHeldItem(player, offHand, null);
    }

    private ItemStack createPartialItem(ItemStack original, double amount) {
        NBTItem nbtItem = new NBTItem(original);
        String signer = nbtItem.getString("Signer");
        boolean signed = nbtItem.getBoolean("Signed");
        double tax = nbtItem.getDouble("Tax");
        return assetHandler.getItem(signer == null ? "" : signer, amount, 1, signed, tax, assetHandler.getAmountOverrideId(original));
    }

    private void setHeldItem(Player player, boolean offHand, ItemStack replacement) {
        if (replacement == null || replacement.getType() == Material.AIR) {
            if (offHand) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            return;
        }

        if (offHand) {
            player.getInventory().setItemInOffHand(replacement);
        } else {
            player.getInventory().setItemInMainHand(replacement);
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return;
        }

        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private static final class RedemptionResult {
        private final int redeemedAmount;
        private final int totalTax;
        private final int displayAmount;
        private final int displayTax;
        private final int loggedStackSize;
        private final int messageStackSize;

        private RedemptionResult(int redeemedAmount, int totalTax, int displayAmount, int displayTax, int loggedStackSize, int messageStackSize) {
            this.redeemedAmount = redeemedAmount;
            this.totalTax = totalTax;
            this.displayAmount = displayAmount;
            this.displayTax = displayTax;
            this.loggedStackSize = loggedStackSize;
            this.messageStackSize = messageStackSize;
        }

        private static RedemptionResult none() {
            return new RedemptionResult(0, 0, 0, 0, 0, 1);
        }
    }
}
