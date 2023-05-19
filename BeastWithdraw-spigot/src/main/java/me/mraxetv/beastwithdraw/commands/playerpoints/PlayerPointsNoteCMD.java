package me.mraxetv.beastwithdraw.commands.playerpoints;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.AliasesRegistration;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class PlayerPointsNoteCMD extends CommandModule implements CommandExecutor {

    private BeastWithdrawPlugin pl;
    private String message;
    private String handlerID;



    public PlayerPointsNoteCMD(BeastWithdrawPlugin plugin, String handlerID) {
        super(plugin,"BeastWithdraw.PlayerPointsNote.Withdraw",1,2);
        pl = plugin;
        this.handlerID = handlerID;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    AliasesRegistration.setAliases("bpWithdraw", pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getStringList("Settings.Aliases"));
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchFieldException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.runTaskLater(pl,1);

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
            String s = pl.getMessages().getString("Withdraws.PlayerPointsNote.Help");
            s = s.replaceAll("%amount%", "" + pl.getUtils().formatNumber((int) pl.getWithdrawManager().getAssetHandler(handlerID).getBalance(p)));
            s = s.replaceAll("%balance%", "" + pl.getUtils().formatNumber((int) pl.getWithdrawManager().getAssetHandler(handlerID).getBalance(p)));
            s = ChatColor.translateAlternateColorCodes('&', s);
            pl.getUtils().sendMessage(p, s);
            return;
        }
        int takenAmount;

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
        //Player Xp
        int amount = (int) pl.getWithdrawManager().getAssetHandler(handlerID).getBalance(p);

        if ((args.length >= 1)) {
            //Withdraw all
            if (args[0].equalsIgnoreCase("all")) {
                if (!sender.hasPermission(permission+".All")) {
                    pl.getUtils().noPermission(p);
                    return;
                }
                takenAmount = amount;
            }

            //Regular integer check
            else if (!pl.getUtils().isInt(args[0])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%amount%", args[0]);
                pl.getUtils().sendMessage(sender, s);
                return;
            } else {
                takenAmount = Math.abs(Integer.parseInt(args[0]));
            }

            //Drop to floor if there is no empty slot
            if (!pl.getConfig().getBoolean("Settings.WithdrawDropFloor")) {
                if (p.getInventory().firstEmpty() == -1) {
                    message = pl.getMessages().getString("Withdraws.FullInventory");
                    pl.getUtils().sendMessage(p, message);
                    return;
                }
            }
            //Check if amount is bigger then 0
            if ((takenAmount <= 0)) {
                message = pl.getMessages().getString("Withdraws.PlayerPointsNote.Min");
                message = message.replaceAll("%min-amount%", "" + 1);
                message = ChatColor.translateAlternateColorCodes('&', message);
                pl.getUtils().sendMessage(p, message);
                return;
            }
            int minAmount = 0;
            //Limit min and max amount which can be withdrawn
            if (!sender.hasPermission("BeastWithdraw.PlayerPointsNote.ByPass.WithdrawLimit")) {
                minAmount = pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getInt("Settings.Min");
                if (pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                    for (String s : pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                        if (sender.hasPermission("BeastWithdraw.PlayerPointsNote.PermissionNotes." + s)) {
                            minAmount = pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getInt("Settings.PermissionNotes." + s + ".Min");
                        }
                    }

                }
                if ((takenAmount < minAmount)) {
                    message = pl.getMessages().getString("Withdraws.PlayerPointsNote.Min");
                    message = message.replaceAll("%min-amount%", pl.getUtils().formatNumber(minAmount));
                    pl.getUtils().sendMessage(p, message);
                    return;
                }


                int maxXp = pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getInt("Settings.Max");
                if (pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
                    for (String s : pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getConfigurationSection("Settings.PermissionNotes").getKeys(false)) {
                        if (sender.hasPermission("BeastWithdraw.PlayerPointsNote.PermissionNotes." + s)) {
                            maxXp = pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getInt("Settings.PermissionNotes." + s + ".Max");
                        }
                    }
                }

                if ((takenAmount > maxXp)) {
                    message = pl.getMessages().getString("Withdraws.PlayerPointsNote.Max");
                    message = message.replaceAll("%max-amount%", pl.getUtils().formatNumber(maxXp));
                    pl.getUtils().sendMessage(p, message);
                    return;
                }
            }




            if(((double)takenAmount*noteAmount) > Integer.MAX_VALUE){
                message = pl.getMessages().getString("Withdraws.ToBigNumber");
                message = message.replaceAll("%amount%", Utils.formatDouble((double) takenAmount*noteAmount));
                pl.getUtils().sendMessage(p, message);
              return;
            }

            if ((amount < takenAmount * noteAmount)) {
                message = pl.getMessages().getString("Withdraws.PlayerPointsNote.NotEnough");
                message = message.replaceAll("%balance%", "" + pl.getUtils().formatNumber(amount));
                message = message.replaceAll("%taken-amount%", "" + pl.getUtils().formatNumber(takenAmount*noteAmount));
                pl.getUtils().sendMessage(p, message);
                return;
            }

            //Charge Fee
            if (!p.isPermissionSet("BeastWithdraw.PlayerPointsNote.ByPass.Fee")) {
                if (pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getBoolean("Settings.Charges.Fee.Enabled")) {

                    double bal = pl.getWithdrawManager().getAssetHandler(handlerID).getBalance(p);
                    //Money Fee
                    double moneyFee = pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getDouble("Settings.Charges.Fee.Cost");
                    if (bal < moneyFee*noteAmount) {
                        String s = pl.getMessages().getString("Withdraws.CashNote.Fee.NotEnough");
                        s = s.replaceAll("%fee%", "" + pl.getUtils().formatDouble(moneyFee*noteAmount));
                        pl.getUtils().sendMessage(p, s);
                        return;
                    }
                    pl.getWithdrawManager().getAssetHandler(handlerID).withdrawAmount(p, moneyFee*noteAmount);
                    String s = pl.getMessages().getString("Withdraws.CashNote.Fee.TakenFee");
                    s = s.replaceAll("%fee%", "" + pl.getUtils().formatDouble(moneyFee*noteAmount));
                    pl.getUtils().sendMessage(p, s);
                }
            }
            int tax = 0;
            //Charge Tax
            if (!p.isPermissionSet("BeastWithdraw.PlayerPointsNote.ByPass.Tax")) {
                if (pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getBoolean("Settings.Charges.Tax.Enabled")) {
                    double percentage = pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getDouble("Settings.Charges.Tax.Percentage");
                    if (percentage > 100.0) percentage = 100.0;
                    tax = (int) (takenAmount * (percentage / 100));
                    String s = pl.getMessages().getString("Withdraws.PlayerPointsNote.Tax.TakenTax");
                    s = s.replaceAll("%tax%", "" + pl.getUtils().formatNumber(tax * noteAmount));
                    pl.getUtils().sendMessage(p, s);
                }
            }
            pl.getWithdrawManager().getAssetHandler(handlerID).withdrawAmount(p,takenAmount*noteAmount);

            String s = pl.getMessages().getString("Withdraws.PlayerPointsNote.Withdraw");
            s = s.replaceAll("%taken-amount%", "" + pl.getUtils().formatNumber(takenAmount*noteAmount));
            s = s.replaceAll("%balance%", "" + pl.getUtils().formatNumber(XpManager.getTotalExperience(p)));
            Utils.sendMessage(p, s);

            takenAmount = takenAmount - tax;


            ItemStack xpBottle = pl.getWithdrawManager().getAssetHandler(handlerID).getItem(p.getName(), takenAmount, noteAmount, true);
            if (p.getInventory().firstEmpty() != -1) {
                Utils.addItem(p,xpBottle);
            } else {
                p.getWorld().dropItem(p.getLocation(), xpBottle);
            }

            if (pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getBoolean("Settings.Sounds.Withdraw.Enabled")) {
                try {
                    String sound = pl.getWithdrawManager().getAssetHandler(handlerID).getConfig().getString("Settings.Sounds.Withdraw.Sound");
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
