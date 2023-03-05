package me.mraxetv.beastwithdraw.commands.xpbottle;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import me.mraxetv.beastwithdraw.commands.CommandModule;
import me.mraxetv.beastwithdraw.utils.Utils;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

public class XpBottleCMD extends CommandModule implements CommandExecutor {

    private BeastWithdrawPlugin pl;
    String message;
    List<String> messagel;


    public XpBottleCMD(BeastWithdrawPlugin plugin) {
        super(plugin,"BeastWithdraw.XpBottle.Withdraw",1,2);
        pl = plugin;

        try {
            pl.getAliasesManager().setAliases("XpBottle", pl.getWithdrawManager().getXpBottleConfig().getStringList("Settings.Aliases"));
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
            String s = pl.getMessages().getString("Withdraws.XpBottle.Help");
            s = s.replaceAll("%amount%", "" + pl.getUtils().formatNumber(XpManager.getTotalExperience(p)));
            s = s.replaceAll("%balance%", "" + pl.getUtils().formatNumber(XpManager.getTotalExperience(p)));
            s = ChatColor.translateAlternateColorCodes('&', s);
            pl.getUtils().sendMessage(p, s);
            return;
        }
        int takenXp;

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
        //Player Xp
        int xp = XpManager.getTotalExperience(p);

        if ((args.length >= 1)) {
            //Withdraw all
            if (args[0].equalsIgnoreCase("all")) {
                if (!sender.hasPermission(permission+".All")) {
                    pl.getUtils().noPermission(p);
                    return;
                }
                takenXp = XpManager.getTotalExperience(p);
            }
            //Withdraw with levels


            else if(args[0].toLowerCase().endsWith("l") && args[0].toLowerCase().split("l").length == 1 && Utils.isInt(args[0].toLowerCase().split("l")[0])){



                int lv = Math.abs(Integer.parseInt(args[0].toLowerCase().split("l")[0]));

                if(lv > p.getLevel()){
                    String s = pl.getMessages().getString("Withdraws.XpBottle.NotEnoughLevels");
                    s = s.replaceAll("%amount%", ""+lv);
                    pl.getUtils().sendMessage(sender, s);
                    return;
                }
                //Calculating off set
                int ofSetLevel = p.getLevel() - lv;
                int offSetXp = XpManager.getExpToLevel(ofSetLevel);

                takenXp =  XpManager.getExpToLevel(lv+ofSetLevel) - offSetXp;
            }
            //Regular integer check
            else if (!pl.getUtils().isInt(args[0])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%amount%", args[0]);
                pl.getUtils().sendMessage(sender, s);
                return;
            } else {
                takenXp = Math.abs(Integer.parseInt(args[0]));
            }

            //Drop to floor if there is no empty slot
            if (!pl.getConfig().getBoolean("Settings.WithdrawDropFloor")) {
                if (p.getInventory().firstEmpty() == -1) {
                    message = pl.getMessages().getString("Withdraws.FullInventory");
                    pl.getUtils().sendMessage(p, message);
                    return;
                }
            }
            //Check if xp is bigger then 0
            if ((takenXp <= 0)) {
                message = pl.getMessages().getString("Withdraws.XpBottle.Min");
                message = message.replaceAll("%min-amount%", "" + 1);
                message = ChatColor.translateAlternateColorCodes('&', message);
                pl.getUtils().sendMessage(p, message);
                return;
            }
            int minXp = 0;
            //Limit min and max amount of xp which can be withdrawn
            if (!sender.hasPermission("BeastWithdraw.XpBottle.ByPass.WithdrawLimit")) {
                minXp = pl.getWithdrawManager().getXpBottleConfig().getInt("Settings.Min");
                if (pl.getWithdrawManager().getXpBottleConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                    for (String s : pl.getWithdrawManager().getXpBottleConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                        if (sender.hasPermission("BeastWithdraw.XpBottle.PermissionNotes." + s)) {
                            minXp = pl.getWithdrawManager().getXpBottleConfig().getInt("Settings.PermissionNotes." + s + ".Min");
                        }
                    }

                }
                if ((takenXp < minXp)) {
                    message = pl.getMessages().getString("Withdraws.XpBottle.Min");
                    message = message.replaceAll("%min-amount%", pl.getUtils().formatNumber(minXp));
                    pl.getUtils().sendMessage(p, message);
                    return;
                }


                int maxXp = pl.getWithdrawManager().getXpBottleConfig().getInt("Settings.Max");
                if (pl.getWithdrawManager().getXpBottleConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                    for (String s : pl.getWithdrawManager().getXpBottleConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                        if (sender.hasPermission("BeastWithdraw.XpBottle.PermissionNotes." + s)) {
                            maxXp = pl.getWithdrawManager().getXpBottleConfig().getInt("Settings.PermissionNotes." + s + ".Max");
                        }
                    }
                }

                if ((takenXp > maxXp)) {
                    message = pl.getMessages().getString("Withdraws.XpBottle.Max");
                    message = message.replaceAll("%max-amount%", pl.getUtils().formatNumber(maxXp));
                    pl.getUtils().sendMessage(p, message);
                    return;
                }
            }




            if(((double)takenXp*amount) > Integer.MAX_VALUE){
                message = pl.getMessages().getString("Withdraws.ToBigNumber");
                message = message.replaceAll("%amount%", Utils.formatDouble((double) takenXp*amount));
                pl.getUtils().sendMessage(p, message);
              return;
            }

            if ((xp < takenXp * amount)) {
                message = pl.getMessages().getString("Withdraws.XpBottle.NotEnough");
                message = message.replaceAll("%balance%", "" + pl.getUtils().formatNumber(xp));
                message = message.replaceAll("%taken-amount%", "" + pl.getUtils().formatNumber(takenXp*amount));
                pl.getUtils().sendMessage(p, message);
                return;
            }

            //Charge Fee
            if (!p.isPermissionSet("BeastWithdraw.XpBottle.ByPass.Fee")) {
                if (pl.getWithdrawManager().getXpBottleConfig().getBoolean("Settings.Charges.Fee.Enabled")) {


                    //Money Fee
                    double moneyFee = pl.getWithdrawManager().getXpBottleConfig().getDouble("Settings.Charges.Fee.Cost");
                    if (!pl.getEcon().has(p,moneyFee*amount)) {
                        String s = pl.getMessages().getString("Withdraws.CashNote.Fee.NotEnough");
                        s = s.replaceAll("%fee%", "" + pl.getUtils().formatDouble(moneyFee*amount));
                        pl.getUtils().sendMessage(p, s);
                        return;
                    }
                    pl.getEcon().withdrawPlayer(p, moneyFee*amount);
                    String s = pl.getMessages().getString("Withdraws.CashNote.Fee.TakenFee");
                    s = s.replaceAll("%fee%", "" + pl.getUtils().formatDouble(moneyFee*amount));
                    pl.getUtils().sendMessage(p, s);
                }
            }
            int tax = 0;
            //Charge Tax
            if (!p.isPermissionSet("BeastWithdraw.XpBottle.ByPass.Tax")) {
                if (pl.getWithdrawManager().getXpBottleConfig().getBoolean("Settings.Charges.Tax.Enabled")) {
                    double percentage = pl.getWithdrawManager().getXpBottleConfig().getDouble("Settings.Charges.Tax.Percentage");
                    if (percentage > 100.0) percentage = 100.0;
                    tax = (int) (takenXp * (percentage / 100));
                    String s = pl.getMessages().getString("Withdraws.XpBottle.Tax.TakenTax");
                    s = s.replaceAll("%tax%", "" + pl.getUtils().formatNumber(tax * amount));
                    pl.getUtils().sendMessage(p, s);
                }
            }
            XpManager.setTotalExperience(p, (xp - takenXp*amount));

            String s = pl.getMessages().getString("Withdraws.XpBottle.Withdraw");
            s = s.replaceAll("%taken-amount%", "" + pl.getUtils().formatNumber(takenXp*amount));
            s = s.replaceAll("%balance%", "" + pl.getUtils().formatNumber(XpManager.getTotalExperience(p)));
            Utils.sendMessage(p, s);

            takenXp = takenXp - tax;


            ItemStack xpBottle = pl.getItemManger().getXpb(p.getName(), takenXp, amount, true);
            if (p.getInventory().firstEmpty() != -1) {
                Utils.addItem(p,xpBottle);
            } else {
                p.getWorld().dropItem(p.getLocation(), xpBottle);
            }

            if (pl.getWithdrawManager().getXpBottleConfig().getBoolean("Settings.Sounds.Withdraw.Enabled")) {
                try {
                    String sound = pl.getWithdrawManager().getXpBottleConfig().getString("Settings.Sounds.Withdraw.Sound");
                    p.playSound(p.getLocation(), Sound.valueOf(sound), 1f, 1f);

                } catch (Exception e) {
                    Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() + "§cBroken sound in XpBottle Withdraw section!");
                }
            }
        }
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
