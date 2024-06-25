package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.CommandModule;
import me.mraxetv.beastwithdraw.managers.WithdrawManager;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class XpBottleGiveSub extends CommandModule {
    /**
     * @param pl
     * @param permission - The label of the command.
     * @param minArgs    - The minimum amount of arguments.
     * @param maxArgs    - The maximum amount of arguments.
     */
    public XpBottleGiveSub(BeastWithdrawPlugin pl, String permission, int minArgs, int maxArgs) {
        super(pl, permission, minArgs, maxArgs);
    }

    @Override
    public void run(CommandSender sender, String[] args) {


        if (!sender.hasPermission(permission)) {
            String s = pl.getMessages().getString("Withdraws.NoPermission");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            pl.getUtils().sendMessage(sender, s);
            return;
        }

        if (!hasEnoughArgs(args)) {
            String s = pl.getMessages().getString("Withdraws.Admin.XpBottle.GiveCMD");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            pl.getUtils().sendMessage(sender, s);
            return;
        }
        if (!isOnline(args[1])) {
            String s = pl.getMessages().getString("Withdraws.NotOnline");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            s = s.replaceAll("%player%", args[1]);
            pl.getUtils().sendMessage(sender, s);
            return;
        }

        if (!Utils.isInt(args[2])) {
            String s = pl.getMessages().getString("Withdraws.NoNumber");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            s = s.replaceAll("%amount%", args[2]);
            pl.getUtils().sendMessage(sender, s);
            return;
        }
        int amount = 1;

        if (args.length > 3) {
            if (!Utils.isInt(args[3])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%prefix%", Utils.getPrefix());
                s = s.replaceAll("%amount%", args[3]);
                pl.getUtils().sendMessage(sender, s);
                return;
            }
            amount = Integer.parseInt(args[3]);
        }

        boolean signet = false;
        String signer = "";
        if(args.length == 5) {
            signet = true;
            signer = args[4];
        }


        Player target = Bukkit.getPlayer(args[1]);
        int xp = Integer.parseInt(args[2]);

        ItemStack xpBottle = WithdrawManager.XP_BOTTLE.getItem(signer, xp, amount, signet);


            Utils.addItem(target,xpBottle);



        String message = pl.getMessages().getString("Withdraws.XpBottle.RewardReceived");
        message = message.replaceAll("%received-amount%", "" + pl.getUtils().formatNumber(xp)).replaceAll("%note-amount%", Utils.setStackSize(amount));
        pl.getUtils().sendMessage(target, message);

        message = pl.getMessages().getString("Withdraws.Admin.XpBottle.Given");
        message = message.replaceAll("%received-amount%", "" + pl.getUtils().formatNumber(xp)).replaceAll("%note-amount%", Utils.setStackSize(amount)).replaceAll("%player%",target.getName());
        pl.getUtils().sendMessage(sender, message);
        return;


    }


    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
