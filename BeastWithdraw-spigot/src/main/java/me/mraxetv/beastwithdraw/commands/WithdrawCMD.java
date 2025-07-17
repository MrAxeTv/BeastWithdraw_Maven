package me.mraxetv.beastwithdraw.commands;

import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WithdrawCMD extends ShortCommand {

    private final BeastWithdrawPlugin pl;
    private final AssetHandler assetHandler;

    public WithdrawCMD(BeastWithdrawPlugin pl, AssetHandler assetHandler) {
        super(pl, assetHandler.getID(), assetHandler.getConfig().getStringList("Settings.Aliases"), "BeastWithdraw." + assetHandler.getID() + ".Withdraw");
        this.pl = pl;
        this.assetHandler = assetHandler;
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
        if (this.getPermission() == null || target.hasPermission(this.getPermission())) {
            return true;
        }

        pl.getUtils().noPermission((Player) target);
        return false;
    }

    private void handleWithdraw(Player p, String[] args) {
        if (!p.hasPermission(getPermission())) {
            pl.getUtils().noPermission(p);
            return;
        }

        if (args.length == 0) {
            sendHelpMessage(p);
            return;
        }

        int stackSize = parseStackSize(p, args);
        if (stackSize == -1) return;

        double balance = assetHandler.getBalance(p);
        double takenAmount = parseWithdrawAmount(p, args[0], balance);
        if (takenAmount == -1) return;

        if (!validateInventorySpace(p)) return;
        if (!validateWithdrawLimits(p, takenAmount)) return;
        if (!validateBigAmount(p, takenAmount, stackSize)) return;
        if (!validateBalance(p, balance, takenAmount, stackSize)) return;

        if (!chargeFee(p, stackSize)) return;

        performWithdraw(p, takenAmount, stackSize);
    }

    protected void sendHelpMessage(Player p) {
        String helpMessage = assetHandler.getMessageSection().getString("Help");
        helpMessage = helpMessage.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalance(p)));
        helpMessage = ChatColor.translateAlternateColorCodes('&', helpMessage);
        pl.getUtils().sendMessage(p, helpMessage);
    }

    protected int parseStackSize(Player p, String[] args) {
        if (args.length != 2) return 1;

        if (!Utils.isInt(args[1])) {
            String s = pl.getMessages().getString("Withdraws.InvalidNumber");
            s = s.replace("%amount%",args[1]);
            pl.getUtils().sendMessage(p,s);
        }

        int stackSize = Math.abs(Integer.parseInt(args[1]));
        int maxStack = assetHandler.getConfig().getInt("Settings.MaxStackSize", 64);
        if (stackSize > maxStack) {
            String s = assetHandler.getMessageSection().getString("MaxStackSize");
            s = s.replace("%stack%",Utils.formatNumber(stackSize));
            pl.getUtils().sendMessage(p,s);

            return maxStack;
        }

        return Math.max(stackSize, 1);
    }

    protected double parseWithdrawAmount(Player p, String arg, double balance) {
        if (arg.equalsIgnoreCase("all")) {
            if (!p.hasPermission(getPermission() + ".All")) {
                pl.getUtils().noPermission(p);
                return -1;
            }
            return balance;
        }

        if (!Utils.isInt(arg)) {
            String s = pl.getMessages().getString("Withdraws.InvalidNumber");
            s = s.replace("%amount%",arg);
            pl.getUtils().sendMessage(p,s);

            return -1;
        }

        return Math.abs(Double.parseDouble(arg));
    }

    protected boolean validateInventorySpace(Player p) {
        if (pl.getConfig().getBoolean("Settings.WithdrawDropFloor")) return true;

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
            pl.getUtils().sendMessage(p,s);

            return false;
        }

        if (takenAmount > max) {
            String s = assetHandler.getMessageSection().getString("Max");
            s = s.replace("%max-amount%",assetHandler.formatNumber(max));
            pl.getUtils().sendMessage(p,s);
            return false;
        }

        return true;
    }

    private double getPermissionMin(Player p) {
        double min = assetHandler.getConfig().getDouble("Settings.Min");

        if (assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            for (String key : assetHandler.getConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                if (p.isPermissionSet("BeastWithdraw." + assetHandler.getID() + ".PermissionNotes." + key)) {
                    min = assetHandler.getConfig().getDouble("Settings.PermissionNotes." + key + ".Min");
                }
            }
        }
        return min;
    }

    private double getPermissionMax(Player p) {
        double max = assetHandler.getConfig().getDouble("Settings.Max");

        if (assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            for (String key : assetHandler.getConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                if (p.isPermissionSet("BeastWithdraw." + assetHandler.getID() + ".PermissionNotes." + key)) {
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
            pl.getUtils().sendMessage(p, msg);
            return false;
        }
        return true;
    }

    protected boolean chargeFee(Player p, int stackSize) {
        if (p.hasPermission("BeastWithdraw." + assetHandler.getID() + ".ByPass.Fee")) return true;

        if (!assetHandler.getConfig().getBoolean("Settings.Charges.Fee.Enabled")) return true;

        double fee = assetHandler.getConfig().getDouble("Settings.Charges.Fee.Cost") * stackSize;
        double balance = assetHandler.getBalance(p);

        if (balance < fee) {
            String s = assetHandler.getMessageSection().getString("Tax.NotEnough");
            s = s.replace("%amount%",assetHandler.formatWithPreSuffix(fee));
            pl.getUtils().sendMessage(p,s);
            return false;
        }

        assetHandler.withdrawAmount(p, fee);

        String s = assetHandler.getMessageSection().getString("Tax.TakenFee");
        s = s.replace("%fee%",assetHandler.formatWithPreSuffix(fee));
        pl.getUtils().sendMessage(p,s);

        return true;
    }

    protected void performWithdraw(Player p, double takenAmount, int stackSize) {

        assetHandler.withdrawAmount(p, takenAmount * stackSize);

        String s = assetHandler.getMessageSection().getString("Withdraw");
        s = s.replace("%amount%", assetHandler.formatWithPreSuffix(takenAmount));
        s = Utils.formatStackSize(s,stackSize);
        s = s.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalance(p)));
        pl.getUtils().sendMessage(p, s);
        double tax = calculateTax(p);
        ItemStack item = assetHandler.getItem(p.getName(), takenAmount, stackSize, true,tax);
        if (p.getInventory().firstEmpty() != -1) {
            Utils.addItem(p, item);
        } else {
            p.getWorld().dropItem(p.getLocation(), item);
        }

        playWithdrawSound(p);
    }
    protected double calculateTax(Player p){

        if (p.hasPermission("BeastWithdraw." + assetHandler.getID() + ".ByPass.Tax")) return 0;
            double tax = assetHandler.getConfig().getDouble("Settings.Charges.Tax.Percentage");
            if (assetHandler.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                for (String key : assetHandler.getConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                    if (p.isPermissionSet("BeastWithdraw." + assetHandler.getID() + ".PermissionNotes." + key)) {
                        tax = assetHandler.getConfig().getDouble("Settings.PermissionNotes." + key + ".Tax.Percentage");
                    }
                }
            }
            return tax;
    }

    protected void playWithdrawSound(Player p) {
        if (!assetHandler.getConfig().getBoolean("Settings.Sounds.Withdraw.Enabled")) return;

        try {
            String sound = assetHandler.getConfig().getString("Settings.Sounds.Withdraw.Sound");
            p.playSound(p.getLocation(), Sound.valueOf(sound), 1f, 1f);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "§cBroken sound in " + assetHandler.getID() + " Withdraw section!");
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
