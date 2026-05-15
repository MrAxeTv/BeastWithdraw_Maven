package me.mraxetv.beastwithdraw.commands.cashwithdraw;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.managers.assets.CashNoteHandler;
import me.mraxetv.beastwithdraw.utils.MessagesLang;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class CashNoteCMD extends WithdrawCMD {
    private final BeastWithdrawPlugin plugin;
    private final CashNoteHandler assetHandler;

    public CashNoteCMD(BeastWithdrawPlugin pl, CashNoteHandler assetHandler) {
        super(pl, assetHandler);
        this.plugin = pl;
        this.assetHandler = assetHandler;

    }

    @Override
    public boolean execute(CommandSender sender, String cmd, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getUtils().sendMessage(sender, "%prefix% Console can't use this command! /BeastWithdraw help");
            return true;
        }

        handleCashWithdraw((Player) sender, args);
        return true;
    }

    private void handleCashWithdraw(Player player, String[] args) {
        if (!player.hasPermission(getPermission())) {
            plugin.getUtils().noPermission(player);
            return;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return;
        }

        int stackSize = parseStackSize(player, args);
        if (stackSize == -1) {
            return;
        }

        BigDecimal balance = assetHandler.getBalanceDecimal(player);
        BigDecimal feeAmount = getFeeAmount(player, stackSize);
        BigDecimal takenAmount = parseCashWithdrawAmount(player, args[0], balance, feeAmount, stackSize);
        if (takenAmount == null) {
            return;
        }

        if (!validateInventorySpace(player)) return;
        if (!validateCashWithdrawLimits(player, takenAmount)) return;

        BigDecimal totalNoteAmount = takenAmount.multiply(BigDecimal.valueOf(stackSize));
        if (!validateBalanceAfterFee(player, balance, totalNoteAmount, feeAmount)) return;

        CashNoteHandler.CashTransactionResult transaction = assetHandler.withdrawForNote(player, totalNoteAmount, feeAmount);
        if (!transaction.isSuccess()) {
            sendTransactionFailed(player);
            return;
        }

        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            String feeMessage = assetHandler.getMessageSection().getString("Tax.TakenFee");
            feeMessage = feeMessage.replace("%fee%", assetHandler.formatWithPreSuffix(feeAmount.doubleValue()));
            plugin.getUtils().sendMessage(player, feeMessage);
        }

        String message = assetHandler.getMessageSection().getString("Withdraw");
        message = message.replace("%amount%", assetHandler.formatWithPreSuffix(takenAmount.doubleValue()));
        message = message.replace("%stacked-amount%", assetHandler.formatWithPreSuffix(totalNoteAmount.doubleValue()));
        message = Utils.formatStackSize(message, stackSize);
        message = message.replace("%balance%", assetHandler.formatWithPreSuffix(transaction.getBalanceAfter().doubleValue()));
        plugin.getUtils().sendMessage(player, message);

        BigDecimal taxPercentage = BigDecimal.valueOf(calculateTax(player)).setScale(2, RoundingMode.DOWN);
        ItemStack item = assetHandler.getItem(player.getName(), takenAmount.doubleValue(), stackSize, true, taxPercentage.doubleValue());
        if (player.getInventory().firstEmpty() != -1) {
            Utils.addItem(player, item);
        } else {
            player.getWorld().dropItem(player.getLocation(), item);
        }

        plugin.getWithdrawLogger().logWithdraw(assetHandler, player, takenAmount.doubleValue(), stackSize, totalNoteAmount.doubleValue(), transaction.getBalanceAfter().doubleValue());
        playWithdrawSound(player);
    }

    private BigDecimal parseCashWithdrawAmount(Player player, String arg, BigDecimal balance, BigDecimal feeAmount, int stackSize) {
        if (arg.equalsIgnoreCase("all")) {
            if (!player.hasPermission(getPermission() + ".All")) {
                plugin.getUtils().noPermission(player);
                return null;
            }

            BigDecimal availableForNotes = balance.subtract(feeAmount);
            if (availableForNotes.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO.setScale(balance.scale(), RoundingMode.DOWN);
            }

            return assetHandler.normalize(
                    availableForNotes.divide(BigDecimal.valueOf(stackSize), balance.scale(), RoundingMode.DOWN),
                    RoundingMode.DOWN
            );
        }

        if (!Utils.isDouble(arg)) {
            String invalid = plugin.getMessages().getString("Withdraws.InvalidNumber");
            invalid = invalid.replace("%amount%", arg);
            plugin.getUtils().sendMessage(player, invalid);
            return null;
        }

        return assetHandler.parseUserAmount(arg);
    }

    private boolean validateCashWithdrawLimits(Player player, BigDecimal takenAmount) {
        BigDecimal min = getPermissionMin(player);
        BigDecimal max = getPermissionMax(player);

        if (takenAmount.compareTo(BigDecimal.ZERO) <= 0 || takenAmount.compareTo(min) < 0) {
            String message = assetHandler.getMessageSection().getString("Min");
            message = message.replace("%min-amount%", assetHandler.formatNumber(min.doubleValue()));
            plugin.getUtils().sendMessage(player, message);
            return false;
        }

        if (takenAmount.compareTo(max) > 0) {
            String message = assetHandler.getMessageSection().getString("Max");
            message = message.replace("%max-amount%", assetHandler.formatNumber(max.doubleValue()));
            plugin.getUtils().sendMessage(player, message);
            return false;
        }

        return true;
    }

    private boolean validateBalanceAfterFee(Player player, BigDecimal balance, BigDecimal totalNoteAmount, BigDecimal feeAmount) {
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0 && balance.compareTo(feeAmount) < 0) {
            String message = assetHandler.getMessageSection().getString("Tax.NotEnough");
            message = message.replace("%amount%", assetHandler.formatWithPreSuffix(feeAmount.doubleValue()));
            plugin.getUtils().sendMessage(player, message);
            return false;
        }

        BigDecimal required = totalNoteAmount.add(feeAmount);
        if (balance.compareTo(required) < 0) {
            String message = assetHandler.getMessageSection().getString("NotEnough");
            message = message.replace("%balance%", assetHandler.formatWithPreSuffix(balance.doubleValue()));
            message = message.replace("%amount%", assetHandler.formatWithPreSuffix(totalNoteAmount.doubleValue()));
            plugin.getUtils().sendMessage(player, message);
            return false;
        }

        return true;
    }

    private BigDecimal getPermissionMin(Player player) {
        BigDecimal min = assetHandler.getConfiguredAmount("Settings.Min");
        if (!assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            return min;
        }

        for (String key : assetHandler.getConfig().getSection("Settings.PermissionNotes").getRoutesAsStrings(false)) {
            if (player.isPermissionSet("BeastWithdraw." + assetHandler.getID() + ".PermissionNotes." + key)) {
                min = assetHandler.getConfiguredAmount("Settings.PermissionNotes." + key + ".Min");
            }
        }

        return min;
    }

    private BigDecimal getPermissionMax(Player player) {
        BigDecimal max = assetHandler.getConfiguredAmount("Settings.Max");
        if (!assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            return max;
        }

        for (String key : assetHandler.getConfig().getSection("Settings.PermissionNotes").getRoutesAsStrings(false)) {
            if (player.isPermissionSet("BeastWithdraw." + assetHandler.getID() + ".PermissionNotes." + key)) {
                max = assetHandler.getConfiguredAmount("Settings.PermissionNotes." + key + ".Max");
            }
        }

        return max;
    }

    private BigDecimal getFeeAmount(Player player, int stackSize) {
        if (player.hasPermission("BeastWithdraw." + assetHandler.getID() + ".ByPass.Fee")) {
            return BigDecimal.ZERO.setScale(assetHandler.getBalanceDecimal(player).scale(), RoundingMode.DOWN);
        }

        if (!assetHandler.getConfig().getBoolean("Settings.Charges.Fee.Enabled")) {
            return BigDecimal.ZERO.setScale(assetHandler.getBalanceDecimal(player).scale(), RoundingMode.DOWN);
        }

        return assetHandler.getConfiguredAmount("Settings.Charges.Fee.Cost")
                .multiply(BigDecimal.valueOf(stackSize))
                .setScale(assetHandler.getBalanceDecimal(player).scale(), RoundingMode.DOWN);
    }

    private void sendTransactionFailed(Player player) {
        plugin.getUtils().sendMessage(player, MessagesLang.TRANSACTION_FAILED);
    }
}
