package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.CommandModule;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class XpBottleGiveAllSub extends CommandModule {
    /**
     * @param pl
     * @param permission - The label of the command.
     * @param minArgs    - The minimum amount of arguments.
     * @param maxArgs    - The maximum amount of arguments.
     */
    public XpBottleGiveAllSub(BeastWithdrawPlugin pl, String permission, int minArgs, int maxArgs) {
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
            String s = pl.getMessages().getString("Withdraws.Admin.XpBottle.GiveAllCMD");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            pl.getUtils().sendMessage(sender, s);
            return;
        }

        if (!Utils.isInt(args[1])) {
            String s = pl.getMessages().getString("Withdraws.NoNumber");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            s = s.replaceAll("%value%", args[1]);
            pl.getUtils().sendMessage(sender, s);
            return;
        }
        int amount = 1;

        if (args.length == 3) {
            if (!Utils.isInt(args[2])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%prefix%", Utils.getPrefix());
                s = s.replaceAll("%value%", args[2]);
                pl.getUtils().sendMessage(sender, s);
                return;
            }
            amount = Integer.parseInt(args[2]);
        }

        boolean signet = false;
        String signer = "";
        if(args.length == 4) {
            signet = true;
            signer = args[3];
        }


        int xp = Integer.parseInt(args[1]);

        ItemStack Xpb = pl.getItemManger().getXpb(signer, xp, amount, signet);

        for (Player target : Bukkit.getOnlinePlayers()) {

            //Add to inventory
            if (target.getInventory().firstEmpty() != -1) {
                target.getInventory().addItem(Xpb);
            }
            //Drop to floor
            else {
                target.getWorld().dropItem(target.getLocation(), Xpb);
            }

            String message = pl.getMessages().getString("Withdraws.XpBottle.RewardReceived");
            message = message.replaceAll("%xp%", "" + pl.getUtils().formatNumber(xp)).replaceAll("%amount%", Utils.setAmount(amount));
            pl.getUtils().sendMessage(target, message);
        }
        String message = pl.getMessages().getString("Withdraws.Admin.XpBottle.GivenToAll");
        message = message.replaceAll("%xp%", "" + pl.getUtils().formatNumber(xp)).replaceAll("%amount%", Utils.setAmount(amount));
        pl.getUtils().sendMessage(sender, message);
        return;

    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
