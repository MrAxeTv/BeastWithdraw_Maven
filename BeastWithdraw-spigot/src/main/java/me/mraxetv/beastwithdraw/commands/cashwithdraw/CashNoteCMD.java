package me.mraxetv.beastwithdraw.commands.cashwithdraw;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.AliasesRegistration;
import me.mraxetv.beastwithdraw.commands.CommandModule;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.WithdrawManager;
import me.mraxetv.beastwithdraw.utils.CurrencyLogger;
import me.mraxetv.beastwithdraw.utils.MessagesLang;
import me.mraxetv.beastwithdraw.utils.Utils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

public class CashNoteCMD extends CommandModule implements CommandExecutor {

    private BeastWithdrawPlugin pl;
    private AssetHandler assetHandler;
    String message;
    List<String> messagel;

    public CashNoteCMD(BeastWithdrawPlugin plugin, AssetHandler assetHandler) {
        super(plugin, "BeastWithdraw.CashNote.Withdraw", 1, 2);
        pl = plugin;
        this.assetHandler = assetHandler;
        try {
            AliasesRegistration.setAliases("bWithdraw", assetHandler.getConfig().getStringList("Settings.Aliases"));
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            Utils.sendMessage(sender, "%prefix% Console can't use this command! /BeastWithdraw help");
            return true;
        }
        run(sender, args);
        return true;
    }


    @Override
    public void run(CommandSender sender, String[] args) {

        Player p = (Player) sender;

        CurrencyLogger.logWithdraw("MrAxeTv", UUID.randomUUID(),"XP",25.0,5, 1,1,1);
        CurrencyLogger.logWithdraw("MrAxeTv", UUID.randomUUID(),"XP",25.0,5, 10.555,10,10);
        //Check if has permission!
        if (!sender.hasPermission(permission)) {
            pl.getUtils().noPermission(p);
            return;
        }

        if (!hasEnoughArgs(args)) {
            String s = pl.getMessages().getString("Withdraws.CashNote.Help");
            s = s.replaceAll("%balance%", "" + pl.getUtils().formatDouble(pl.getEcon().getBalance(p)));
            s = ChatColor.translateAlternateColorCodes('&', s);
            pl.getUtils().sendMessage(p, s);
            return;
        }
        double takenCash;

        int amount = 1;

        if (args.length == 2) {
            if (!Utils.isInt(args[1])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%prefix%", Utils.getPrefix());
                s = s.replaceAll("%amount%", args[1]);
                pl.getUtils().sendMessage(sender, s);
                return;
            }
            amount = Math.abs(Integer.parseInt(args[1]));
        }


        if ((args.length >= 1)) {
            //Withdraw all
            if (args[0].equalsIgnoreCase("all")) {
                if (!sender.hasPermission(permission + ".All")) {
                    pl.getUtils().noPermission(p);
                    return;
                }
                takenCash = new Double(Utils.df2.format(pl.getEcon().getBalance(p)));
            }
            //Regular double check
            else if (!pl.getUtils().isDouble(args[0])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%amount%", args[0]);
                pl.getUtils().sendMessage(sender, s);
                return;
            } else {
                takenCash = Double.parseDouble(Utils.df2.format(Double.parseDouble(args[0])));
            }

            //Drop to floor if there is no empty slot
            if (!pl.getConfig().getBoolean("Settings.WithdrawDropFloor")) {
                if (p.getInventory().firstEmpty() == -1) {
                    message = pl.getMessages().getString("Withdraws.FullInventory");
                    pl.getUtils().sendMessage(p, message);
                    return;
                }
            }
            //Check if cash is bigger then 0
            if ((takenCash <= 0)) {
                message = pl.getMessages().getString("Withdraws.CashNote.Min");
                message = message.replaceAll("%min-amount%", "" + 1);
                message = ChatColor.translateAlternateColorCodes('&', message);
                pl.getUtils().sendMessage(p, message);
                return;
            }
            double minCash = 0;
            //Limit min and max amount of xp which can be withdrawn
            if (!sender.hasPermission("BeastWithdraw.CashNote.ByPass.WithdrawLimit")) {
                minCash = pl.getWithdrawManager().CASH_NOTE.getConfig().getDouble("Settings.Min");
                if (pl.getWithdrawManager().CASH_NOTE.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                    for (String s : pl.getWithdrawManager().CASH_NOTE.getConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                        if (sender.hasPermission("BeastWithdraw.CashNote.PermissionNotes." + s)) {
                            minCash = pl.getWithdrawManager().CASH_NOTE.getConfig().getDouble("Settings.PermissionNotes." + s + ".Min");
                        }
                    }

                }
                if ((takenCash < minCash)) {
                    message = pl.getMessages().getString("Withdraws.CashNote.Min");
                    message = message.replaceAll("%min-amount%", pl.getUtils().formatDouble(pl.getConfig().getDouble("Settings.Min")));
                    pl.getUtils().sendMessage(p, message);
                    return;
                }


                double maxCash = pl.getWithdrawManager().CASH_NOTE.getConfig().getDouble("Settings.Max");
                if (pl.getWithdrawManager().CASH_NOTE.getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                    for (String s : pl.getWithdrawManager().CASH_NOTE.getConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                        if (sender.hasPermission("BeastWithdraw.CashNote.PermissionNotes." + s)) {
                            maxCash = pl.getWithdrawManager().CASH_NOTE.getConfig().getDouble("Settings.PermissionNotes." + s + ".Max");
                        }
                    }
                }

                if ((takenCash > maxCash)) {
                    message = pl.getMessages().getString("Withdraws.CashNote.Max");
                    message = message.replaceAll("%max-amount%", pl.getUtils().formatDouble(maxCash));
                    pl.getUtils().sendMessage(p, message);
                    return;
                }
            }
            double balance = pl.getEcon().getBalance(p);

            if (!pl.getEcon().has(p, takenCash * amount)) {
                message = pl.getMessages().getString("Withdraws.CashNote.NotEnough");
                message = message.replaceAll("%balance%", "" + pl.getUtils().formatDouble(balance));
                message = message.replaceAll("%taken-amount%", "" + pl.getUtils().formatDouble(takenCash * amount));
                pl.getUtils().sendMessage(p, message);
                return;
            }

            //Charge Fee
            if (!p.isPermissionSet("BeastWithdraw.CashNote.ByPass.Fee")) {
                if (pl.getWithdrawManager().CASH_NOTE.getConfig().getBoolean("Settings.Charges.Fee.Enabled")) {


                    //Money Fee
                    double moneyFee = pl.getWithdrawManager().CASH_NOTE.getConfig().getDouble("Settings.Charges.Fee.Cost");
                    //lower the amount for fee in case 'all' argument is used!!
                    if (args[0].equalsIgnoreCase("all")) takenCash = takenCash - moneyFee;

                    if (!pl.getEcon().has(p, takenCash * amount + moneyFee * amount)) {
                        String s = pl.getMessages().getString("Withdraws.CashNote.Fee.NotEnough");
                        s = s.replaceAll("%fee%", "" + pl.getUtils().formatDouble(moneyFee * amount));
                        pl.getUtils().sendMessage(p, s);
                        return;
                    }
                    pl.getEcon().withdrawPlayer(p, moneyFee * amount);
                    String s = pl.getMessages().getString("Withdraws.CashNote.Fee.TakenFee");
                    s = s.replaceAll("%fee%", "" + pl.getUtils().formatDouble(moneyFee * amount));
                    pl.getUtils().sendMessage(p, s);
                }
            }
            double tax = 0;
            //Charge Tax
            if (!p.isPermissionSet("BeastWithdraw.CashNote.ByPass.Tax")) {
                if (pl.getWithdrawManager().CASH_NOTE.getConfig().getBoolean("Settings.Charges.Tax.Enabled")) {
                    double percentage = pl.getWithdrawManager().CASH_NOTE.getConfig().getDouble("Settings.Charges.Tax.Percentage");
                    if (percentage > 100.0) percentage = 100.0;
                    tax = (takenCash * (percentage / 100));
                    String s = pl.getMessages().getString("Withdraws.CashNote.Tax.TakenTax");
                    s = s.replaceAll("%tax%", "" + pl.getUtils().formatDouble(tax * amount));
                    pl.getUtils().sendMessage(p, s);
                }
            }
            EconomyResponse economyResponse = pl.getEcon().withdrawPlayer(p, takenCash * amount);

            if(!economyResponse.transactionSuccess()){
                pl.getServer().getLogger().severe("["+pl.getDescription().getFullName()+"] Withdrawing CashNote has failed: "+economyResponse.errorMessage);
                Utils.sendMessage(p, MessagesLang.TRANSACTION_FAILED);
                return;
            }


            String s = pl.getMessages().getString("Withdraws.CashNote.Withdraw");
            s = s.replaceAll("%taken-amount%", "" + pl.getUtils().formatDouble(takenCash * amount));
            s = s.replaceAll("%balance%", "" + Utils.formatDouble(pl.getEcon().getBalance(p)));
            Utils.sendMessage(p, s);

            takenCash = takenCash - tax;

            CurrencyLogger.logWithdraw(p.getName(),p.getUniqueId(), WithdrawManager.CASH_NOTE.getID(),takenCash,amount,p.getLocation().getBlockX(),p.getLocation().getBlockY(),p.getLocation().getBlockZ());

            //Give Cash Note
            ItemStack cashNote = pl.getItemManger().getCashNote(p.getName(), takenCash, amount, true);
            if (p.getInventory().firstEmpty() != -1) {
                //p.getInventory().addItem(cashNote);
                Utils.addItem(p, cashNote);
            } else {
                p.getWorld().dropItem(p.getLocation(), cashNote);
            }

            //Play Sound
            if (pl.getWithdrawManager().CASH_NOTE.getConfig().getBoolean("Settings.Sounds.Withdraw.Enabled")) {
                try {
                    String sound = pl.getWithdrawManager().CASH_NOTE.getConfig().getString("Settings.Sounds.Withdraw.Sound");
                    p.playSound(p.getLocation(), Sound.valueOf(sound), 1f, 1f);

                } catch (Exception e) {
                    Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "§cBroken sound in CashNote Withdraw section!");
                }
            }
        }
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
