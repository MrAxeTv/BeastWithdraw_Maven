package me.mraxetv.beastwithdraw.commands.tokenwithdraw;

import me.mraxetv.beasttokens.BeastTokensAPI;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.CommandModule;
import me.mraxetv.beastwithdraw.utils.Utils;
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

public class BeastTokenNoteCMD extends CommandModule implements CommandExecutor {

    private BeastWithdrawPlugin pl;
    String message;
    List<String> messagel;

    public BeastTokenNoteCMD(BeastWithdrawPlugin plugin) {
        super(plugin,"BeastWithdraw.BeastTokensNote.Withdraw",1,2);
        pl = plugin;

        try {
            pl.getAliasesManager().setAliases("btWithdraw", pl.getWithdrawManager().getTokensNoteConfig().getStringList("Settings.Aliases"));
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            Utils.sendMessage(sender,"%prefix% Console can't use this command! /BeastWithdraw help");
            return true;
        }
        run(sender,args);
        return true;
    }


    @Override
    public void run(CommandSender sender, String[] args) {

        Player p = (Player) sender;

        //Check if has permission!
        if (!sender.hasPermission(permission)) {
            pl.getUtils().noPermission(p);
            return;
        }

        if(!hasEnoughArgs(args)) {
            String s = pl.getMessages().getString("Withdraws.BeastTokensNote.Help");
            s = s.replaceAll("%balance%", "" + pl.getUtils().formatDouble(BeastTokensAPI.getTokensManager().getTokens(p)));
            s = ChatColor.translateAlternateColorCodes('&', s);
            pl.getUtils().sendMessage(p, s);
            return;
        }
        double takenAmount;

        int noteAmount = 1;

        if (args.length == 2) {
            if (!Utils.isInt(args[1])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%prefix%", Utils.getPrefix());
                s = s.replaceAll("%amount%", args[1]);
                pl.getUtils().sendMessage(sender, s);
                return;
            }
            noteAmount = Math.abs(Integer.parseInt(args[1]));
        }


        if ((args.length >= 1)) {
            //Withdraw all
            if (args[0].equalsIgnoreCase("all")) {
                if (!sender.hasPermission(permission+".All")) {
                    pl.getUtils().noPermission(p);
                    return;
                }
                takenAmount = new Double(Utils.df2.format(BeastTokensAPI.getTokensManager().getTokens(p)));
            }
            //Regular double check
            else if (!pl.getUtils().isDouble(args[0])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%amount%", args[0]);
                pl.getUtils().sendMessage(sender, s);
                return;
            } else {
                takenAmount = Double.parseDouble(Utils.df2.format(Double.parseDouble(args[0])));
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
            if ((takenAmount <= 0)) {
                message = pl.getMessages().getString("Withdraws.BeastTokensNote.Min");
                message = message.replaceAll("%min-amount%", "" + 1);
                message = ChatColor.translateAlternateColorCodes('&', message);
                pl.getUtils().sendMessage(p, message);
                return;
            }
            double minCash = 0;
            //Limit min and max noteAmount of xp which can be withdrawn
            if (!sender.hasPermission("BeastWithdraw.BeastTokensNote.ByPass.WithdrawLimit")) {
                minCash = pl.getWithdrawManager().getTokensNoteConfig().getDouble("Settings.Min");
                if (pl.getWithdrawManager().getTokensNoteConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                    for (String s : pl.getWithdrawManager().getTokensNoteConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                        if (sender.hasPermission("BeastWithdraw.BeastTokensNote.PermissionNotes." + s)) {
                            minCash = pl.getWithdrawManager().getTokensNoteConfig().getDouble("Settings.PermissionNotes." + s + ".Min");
                        }
                    }

                }
                if ((takenAmount < minCash)) {
                    message = pl.getMessages().getString("Withdraws.BeastTokensNote.Min");
                    message = message.replaceAll("%min-amount%", pl.getUtils().formatDouble(pl.getConfig().getDouble("Settings.Min")));
                    pl.getUtils().sendMessage(p, message);
                    return;
                }


                double maxAmount = pl.getWithdrawManager().getTokensNoteConfig().getDouble("Settings.Max");
                if (pl.getWithdrawManager().getTokensNoteConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                    for (String s : pl.getWithdrawManager().getTokensNoteConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                        if (sender.hasPermission("BeastWithdraw.BeastTokensNote.PermissionNotes." + s)) {
                            maxAmount = pl.getWithdrawManager().getTokensNoteConfig().getDouble("Settings.PermissionNotes." + s + ".Max");
                        }
                    }
                }

                if ((takenAmount > maxAmount)) {
                    message = pl.getMessages().getString("Withdraws.BeastTokensNote.Max");
                    message = message.replaceAll("%max-amount%", pl.getUtils().formatDouble(maxAmount));
                    pl.getUtils().sendMessage(p, message);
                    return;
                }
            }
            double balance = BeastTokensAPI.getTokensManager().getTokens(p);

            if ((balance < takenAmount * noteAmount)) {
                message = pl.getMessages().getString("Withdraws.BeastTokensNote.NotEnough");
                message = message.replaceAll("%balance%", ""+pl.getUtils().formatDouble(balance));
                message = message.replaceAll("%taken-amount%", ""+pl.getUtils().formatDouble(takenAmount *noteAmount));;
                pl.getUtils().sendMessage(p, message);
                return;
            }

            //Charge Fee
            if (!p.isPermissionSet("BeastWithdraw.BeastTokensNote.ByPass.Fee")) {
                if (pl.getWithdrawManager().getTokensNoteConfig().getBoolean("Settings.Charges.Fee.Enabled")) {
                    

                    //Fee
                    double fee = pl.getWithdrawManager().getTokensNoteConfig().getDouble("Settings.Charges.Fee.Cost");
                    //lower the noteAmount for fee in case 'all' argument is used!!
                    if(args[0].equalsIgnoreCase("all")) takenAmount = takenAmount - fee;

                    if (balance < takenAmount *noteAmount + fee *noteAmount) {
                        String s = pl.getMessages().getString("Withdraws.BeastTokensNote.Fee.NotEnough");
                        s = s.replaceAll("%fee%", "" + pl.getUtils().formatDouble(fee *noteAmount));
                        pl.getUtils().sendMessage(p, s);
                        return;
                    }
                    pl.getEcon().withdrawPlayer(p, fee *noteAmount);
                    String s = pl.getMessages().getString("Withdraws.BeastTokensNote.Fee.TakenFee");
                    s = s.replaceAll("%fee%", "" + pl.getUtils().formatDouble(fee *noteAmount));
                    pl.getUtils().sendMessage(p, s);
                }
            }
            double tax = 0;
            //Charge Tax
            if (!p.isPermissionSet("BeastWithdraw.BeastTokensNote.ByPass.Tax")) {
                if (pl.getWithdrawManager().getTokensNoteConfig().getBoolean("Settings.Charges.Tax.Enabled")) {
                    double percentage = pl.getWithdrawManager().getTokensNoteConfig().getDouble("Settings.Charges.Tax.Percentage");
                    if (percentage > 100.0) percentage = 100.0;
                    tax = (takenAmount * (percentage / 100));
                    String s = pl.getMessages().getString("Withdraws.BeastTokensNote.Tax.TakenTax");
                    s = s.replaceAll("%tax%", "" + pl.getUtils().formatDouble(tax * noteAmount));
                    pl.getUtils().sendMessage(p, s);
                }
            }
            BeastTokensAPI.getTokensManager().removeTokens(p,takenAmount*noteAmount);
            
            String s = pl.getMessages().getString("Withdraws.BeastTokensNote.Withdraw");
            s = s.replaceAll("%taken-amount%", "" + pl.getUtils().formatDouble(takenAmount *noteAmount));
            s = s.replaceAll("%balance%", "" + Utils.formatDouble(BeastTokensAPI.getTokensManager().getTokens(p)));
            Utils.sendMessage(p, s);

            takenAmount = takenAmount - tax;
            
            ItemStack beastTokensNote = pl.getItemManger().getBTokensNote(p.getName(), takenAmount, noteAmount, true);
            if (p.getInventory().firstEmpty() != -1) {
                Utils.addItem(p,beastTokensNote);
            } else {
                p.getWorld().dropItem(p.getLocation(), beastTokensNote);

            }

            if (pl.getWithdrawManager().getTokensNoteConfig().getBoolean("Settings.Sounds.Withdraw.Enabled")) {
                try {
                    String sound = pl.getWithdrawManager().getTokensNoteConfig().getString("Settings.Sounds.Withdraw.Sound");
                    p.playSound(p.getLocation(), Sound.valueOf(sound), 1f, 1f);

                } catch (Exception e) {
                    Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "§cBroken sound in BeastTokensNote Withdraw section!");
                }
            }
        }
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
