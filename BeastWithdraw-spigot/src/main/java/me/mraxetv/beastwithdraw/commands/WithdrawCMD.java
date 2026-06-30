package me.mraxetv.beastwithdraw.commands;

import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beastlib.utils.LegacyCommandUtils;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.NoteItemSettings;
import me.mraxetv.beastwithdraw.managers.WithdrawItemRequirement;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WithdrawCMD extends ShortCommand {

    private final BeastWithdrawPlugin pl;
    private final AssetHandler assetHandler;
    // generate instace of current time

    public WithdrawCMD(BeastWithdrawPlugin pl, AssetHandler assetHandler) {
        super(pl, assetHandler.getCommandName(), assetHandler.getAliases(), "BeastWithdraw." + assetHandler.getID() + ".Withdraw");
        this.pl = pl;
        this.assetHandler = assetHandler;
        LegacyCommandUtils.applyLegacyCommandFix(pl,this);
    }

    @Override
    public boolean execute(CommandSender sender, String cmd, String[] args) {
        if (!(sender instanceof Player)) {
            pl.getUtils().sendMessage(sender, "%prefix% Console can't use this command! /BeastWithdraw help");
            return true;
        }

        handleWithdraw((Player) sender, args);
        return true;
    }

    @Override
    public boolean testPermission(CommandSender target) {
        if (this.getPermission() == null || !(target instanceof Player) || assetHandler.hasWithdrawPermission((Player) target)) {
            return true;
        }

        pl.getUtils().noPermission((Player) target);
        return false;
    }

    private void handleWithdraw(Player p, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(p);
            return;
        }

        int stackSize = parseStackSize(p, args);
        if (stackSize == -1) return;

        withdraw(p, args[0], stackSize, null);
    }

    public boolean withdraw(Player p, String amountInput, int stackSize, WithdrawItemRequirement.ItemSource itemSource) {
        if (!assetHandler.hasWithdrawPermission(p)) {
            pl.getUtils().noPermission(p);
            return false;
        }
        stackSize = normalizeStackSize(p, stackSize);

        double balance = assetHandler.getBalanceAsDouble(p);
        double takenAmount = parseWithdrawAmount(p, amountInput, balance);
        if (takenAmount == -1) return false;

        if (!validateInventorySpace(p)) return false;
        if (!validateWithdrawLimits(p, takenAmount)) return false;
        if (!validateBigAmount(p, takenAmount, stackSize)) return false;
        if (!validateBalance(p, balance, takenAmount, stackSize)) return false;

        WithdrawItemRequirement.ConsumeResult requiredItem = consumeRequiredItem(p, stackSize, itemSource);
        if (!requiredItem.isSuccess()) return false;

        if (!chargeFee(p, stackSize)) {
            requiredItem.rollback(p);
            return false;
        }

        performWithdraw(p, takenAmount, stackSize);
        return true;
    }

    protected void sendHelpMessage(Player p) {
        String helpMessage = assetHandler.getMessageSection().getString("Help");
        helpMessage = helpMessage.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalanceAsDouble(p)));
        helpMessage = assetHandler.applyPlaceholders(helpMessage, p);
        helpMessage = ChatColor.translateAlternateColorCodes('&', helpMessage);
        pl.getUtils().sendMessage(p, helpMessage);
    }

    protected int parseStackSize(Player p, String[] args) {
        if (args.length != 2) return 1;

        if (!Utils.isInt(args[1])) {
            String s = pl.getMessages().getString("Withdraws.InvalidNumber");
            s = s.replace("%amount%",args[1]);
            pl.getUtils().sendMessage(p,s);
            return -1;
        }

        int stackSize = Math.abs(Integer.parseInt(args[1]));
        int maxStack = assetHandler.getConfig().getInt("Settings.MaxStackSize", 64);
        if (stackSize > maxStack) {
            String s = assetHandler.getMessageSection().getString("MaxStackSize");
            s = s.replace("%stack%",Utils.formatNumber(maxStack));
            s = assetHandler.applyPlaceholders(s, p);
            pl.getUtils().sendMessage(p,s);

            return maxStack;
        }

        return Math.max(stackSize, 1);
    }

    protected int normalizeStackSize(Player p, int stackSize) {
        int normalized = Math.max(stackSize, 1);
        int maxStack = assetHandler.getConfig().getInt("Settings.MaxStackSize", 64);
        if (normalized > maxStack) {
            String s = assetHandler.getMessageSection().getString("MaxStackSize");
            s = s.replace("%stack%", Utils.formatNumber(maxStack));
            s = assetHandler.applyPlaceholders(s, p);
            pl.getUtils().sendMessage(p, s);
            return Math.max(maxStack, 1);
        }
        return normalized;
    }

    protected double parseWithdrawAmount(Player p, String arg, double balance) {
        if (arg.equalsIgnoreCase("all")) {
            if (!assetHandler.hasWithdrawAllPermission(p)) {
                pl.getUtils().noPermission(p);
                return -1;
            }
            return balance;
        }

        if (!Utils.isDouble(arg)) {
            String s = pl.getMessages().getString("Withdraws.InvalidNumber");
            s = s.replace("%amount%",arg);
            pl.getUtils().sendMessage(p,s);

            return -1;
        }
        int scale = 2;
        if(assetHandler.getConfig().getBoolean("Settings.DisableDecimals")) {
            scale = 0;
        }
        BigDecimal bd = new BigDecimal(arg)
                .setScale(scale, RoundingMode.DOWN); // truncate

        return bd.doubleValue();
    }

    protected boolean validateInventorySpace(Player p) {
        if (pl.getSettings().getBoolean("Settings.WithdrawDropFloor")) return true;

        if (p.getInventory().firstEmpty() == -1) {
            String s = pl.getMessages().getString("Withdraws.FullInventory");
            pl.getUtils().sendMessage(p,s);
            return false;
        }
        return true;
    }

    protected boolean validateWithdrawLimits(Player p, double takenAmount) {
        double min = getPermissionMin(p);
       double max = getPermissionMax(p);

        if (takenAmount <= 0 || takenAmount < min) {
            String s = assetHandler.getMessageSection().getString("Min");
            s = s.replace("%min-amount%",assetHandler.formatNumber(min));
            s = assetHandler.applyPlaceholders(s, p);
            pl.getUtils().sendMessage(p,s);

            return false;
        }

        if (takenAmount > max) {
            String s = assetHandler.getMessageSection().getString("Max");
            s = s.replace("%max-amount%",assetHandler.formatNumber(max));
            s = assetHandler.applyPlaceholders(s, p);
            pl.getUtils().sendMessage(p,s);
            return false;
        }

        return true;
    }

    private double getPermissionMin(Player p) {
        double min = assetHandler.getConfig().getDouble("Settings.Min");

        if (assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            for (String key : assetHandler.getConfig().getSection("Settings.PermissionNotes").getRoutesAsStrings(false)) {
                if (assetHandler.hasPermissionNote(p, key)) {
                    min = assetHandler.getConfig().getDouble("Settings.PermissionNotes." + key + ".Min");
                }
            }
        }
        return min;
    }

    private double getPermissionMax(Player p) {
        double max = assetHandler.getConfig().getDouble("Settings.Max");

        if (assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            for (String key : assetHandler.getConfig().getSection("Settings.PermissionNotes").getRoutesAsStrings(false)) {
                if (assetHandler.hasPermissionNote(p, key)) {
                    max = assetHandler.getConfig().getDouble("Settings.PermissionNotes." + key + ".Max");
                }
            }
        }
        return max;
    }

    protected boolean validateBigAmount(Player p, double takenAmount, int stackSize) {
        if (assetHandler.isToBigAmount(takenAmount * stackSize)) {
            String s = assetHandler.getMessageSection().getString("ToBigNumber");
            s = s.replace("%amount%",assetHandler.formatWithPreSuffix(takenAmount*stackSize));
            s = assetHandler.applyPlaceholders(s, p);
            pl.getUtils().sendMessage(p,s);
            return false;
        }
        return true;
    }

    protected boolean validateBalance(Player p, double balance, double takenAmount, int stackSize) {
        if (balance < takenAmount * stackSize) {
            String msg = assetHandler.getMessageSection().getString("NotEnough");
            msg = msg.replace("%balance%", assetHandler.formatWithPreSuffix(balance));
            msg = msg.replace("%amount%", assetHandler.formatWithPreSuffix(takenAmount * stackSize));
            msg = assetHandler.applyPlaceholders(msg, p);
            pl.getUtils().sendMessage(p, msg);
            return false;
        }
        return true;
    }

    protected boolean chargeFee(Player p, int stackSize) {
        if (assetHandler.hasBypassFeePermission(p)) return true;

        if (!assetHandler.getConfig().getBoolean("Settings.Charges.Fee.Enabled")) return true;

        double fee = assetHandler.getConfig().getDouble("Settings.Charges.Fee.Cost") * stackSize;
        double balance = assetHandler.getBalanceAsDouble(p);

        if (balance < fee) {
            String s = assetHandler.getMessageSection().getString("Fee.NotEnough");
            if (s == null) s = assetHandler.getMessageSection().getString("Tax.NotEnough");
            s = s.replace("%amount%",assetHandler.formatWithPreSuffix(fee));
            s = s.replace("%fee%",assetHandler.formatWithPreSuffix(fee));
            s = assetHandler.applyPlaceholders(s, p);
            pl.getUtils().sendMessage(p,s);
            return false;
        }

        assetHandler.withdrawAmount(p, fee);

        String s = assetHandler.getMessageSection().getString("Fee.TakenFee");
        if (s == null) s = assetHandler.getMessageSection().getString("Tax.TakenFee");
        s = s.replace("%fee%",assetHandler.formatWithPreSuffix(fee));
        s = assetHandler.applyPlaceholders(s, p);
        pl.getUtils().sendMessage(p,s);

        return true;
    }

    protected WithdrawItemRequirement.ConsumeResult consumeRequiredItem(Player player, int stackSize, WithdrawItemRequirement.ItemSource source) {
        WithdrawItemRequirement requirement = assetHandler.getWithdrawItemRequirement();
        WithdrawItemRequirement.ItemSource itemSource = source == null
                ? WithdrawItemRequirement.playerInventory(player)
                : source;
        return requirement.consume(player, itemSource, stackSize);
    }

    protected void performWithdraw(Player p, double takenAmount, int stackSize) {

        assetHandler.withdrawAmount(p, takenAmount * stackSize);

        String s = assetHandler.getMessageSection().getString("Withdraw");
        s = s.replace("%amount%", assetHandler.formatWithPreSuffix(takenAmount));
        s = s.replace("%stacked-amount%", assetHandler.formatWithPreSuffix(takenAmount* stackSize));
        s = Utils.formatStackSize(s,stackSize);
        s = s.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalanceAsDouble(p)));
        s = assetHandler.applyPlaceholders(s, p);
        pl.getUtils().sendMessage(p, s);
        double tax = calculateTax(p);
        ItemStack item = assetHandler.getItem(p.getName(), takenAmount, stackSize, true,tax);
        if (p.getInventory().firstEmpty() != -1) {
            Utils.addItem(p, item);
        } else {
            p.getWorld().dropItem(p.getLocation(), item);
        }

        pl.getWithdrawLogger().logWithdraw(assetHandler, p, takenAmount, stackSize, takenAmount * stackSize, assetHandler.getBalanceAsDouble(p));

        playWithdrawSound(p, takenAmount);
    }
    protected double calculateTax(Player p){

        if (assetHandler.hasBypassTaxPermission(p)) return 0;
            double tax = assetHandler.getConfig().getDouble("Settings.Tax.Percentage");
        if (assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                for (String key : assetHandler.getConfig().getSection("Settings.PermissionNotes").getRoutesAsStrings(false)) {
                    if (assetHandler.hasPermissionNote(p, key)) {
                        tax = assetHandler.getConfig().getDouble("Settings.PermissionNotes." + key + ".Tax.Percentage");
                    }
                }
            }
            return tax;
    }

    protected void playWithdrawSound(Player p) {
        playWithdrawSound(p, 0);
    }

    protected void playWithdrawSound(Player p, double amount) {
        NoteItemSettings.SoundSettings soundSettings = assetHandler.getSoundSettings("Withdraw", amount);
        if (soundSettings == null || !soundSettings.isEnabled()) return;

        try {
            Sound sound = Sound.valueOf(soundSettings.getSound().toUpperCase());
            p.playSound(p.getLocation(), sound, soundSettings.getVolume(), soundSettings.getPitch());
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(
                    pl.getUtils().getPrefix() + "\u00A7cBroken sound in " + assetHandler.getID() + " Withdraw section!");
        }
    }




    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("all");
            // Optionally add common numeric suggestions
            suggestions.add("100");
            suggestions.add("1000");
            suggestions.add("10000");
        } else if (args.length == 2) {
            // Suggest stack sizes
            suggestions.add("1");
            suggestions.add("5");
            suggestions.add("10");
            suggestions.add("64");
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }



}
