package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.CommandModule;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GiveNoteSub extends CommandModule {
    /**
     * @param pl
     * @param permission - The label of the command.
     * @param minArgs    - The minimum amount of arguments.
     * @param maxArgs    - The maximum amount of arguments.
     */
    public GiveNoteSub(BeastWithdrawPlugin pl, String permission, int minArgs, int maxArgs) {
        super(pl, permission, minArgs, maxArgs);
    }

    @Override
    public void run(CommandSender sender, String[] args){


        if (!sender.hasPermission(permission)) {
            String s = pl.getMessages().getString("Withdraws.NoPermission");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            pl.getUtils().sendMessage(sender, s);
            return;
        }

        if (!hasEnoughArgs(args)) {
            String s = pl.getMessages().getString("Withdraws.GiveCMD");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            pl.getUtils().sendMessage(sender, s);
            return;
        }
        if (!isOnline(args[2])) {
            String s = pl.getMessages().getString("Withdraws.NotOnline");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            s = s.replaceAll("%player%", args[1]);
            pl.getUtils().sendMessage(sender, s);
            return;
        }
        if(!pl.getWithdrawManager().hasAssetHandler(args[1])){
            String s = pl.getMessages().getString("Withdraws.WrongTypeName");
            s = s.replaceAll("%prefix%", Utils.getPrefix()).replaceAll("%type%",args[1]);
            s = s.replaceAll("%player%", args[1]);
            pl.getUtils().sendMessage(sender, s);
            return;
        }

        if (!Utils.isDouble(args[3])) {
            String s = pl.getMessages().getString("Withdraws.NoNumber");
            s = s.replaceAll("%prefix%", Utils.getPrefix());
            s = s.replaceAll("%amount%", args[2]);
            pl.getUtils().sendMessage(sender, s);
            return;
        }
        int amount = 1;

        if (args.length > 4) {
            if (!Utils.isInt(args[4])) {
                String s = pl.getMessages().getString("Withdraws.NoNumber");
                s = s.replaceAll("%prefix%", Utils.getPrefix());
                s = s.replaceAll("%amount%", args[3]);
                pl.getUtils().sendMessage(sender, s);
                return;
            }
            amount = Integer.parseInt(args[4]);
        }
        boolean signet = false;
        String signer = "";
        if(args.length == 6) {
            signet = true;
            signer = args[5];
        }


        Player target = Bukkit.getPlayer(args[2]);
        double bTokens = Double.parseDouble(args[3]);
        String handlerID = args[1];
        ItemStack item = pl.getWithdrawManager().getAssetHandler(handlerID).getItem(signer, bTokens, amount, signet);

        Utils.addItem(target,item);




        String message = pl.getMessages().getString("Withdraws.BeastTokensNote.RewardReceived");
        message = message.replaceAll("%received-amount%", "" + pl.getUtils().formatDouble(bTokens)).replaceAll("%note-amount%", Utils.setStackSize(amount));
        pl.getUtils().sendMessage(target, message);

        message = pl.getMessages().getString("Withdraws.Admin.BeastTokensNote.Given");
        message = message.replaceAll("%received-amount%", "" + pl.getUtils().formatDouble(bTokens)).replaceAll("%note-amount%", Utils.setStackSize(amount)).replaceAll("%player%",target.getName());
        pl.getUtils().sendMessage(sender, message);
        return;


    }


    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return null;
    }


}
