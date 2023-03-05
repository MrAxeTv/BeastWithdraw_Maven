package me.mraxetv.beastwithdraw.commands.admin;

import me.mraxetv.beastwithdraw.commands.admin.subcmd.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;


public class BeastWithdrawCMD implements CommandExecutor {


    private BeastWithdrawPlugin pl;

    private XpBottleGiveSub xpBottleGiveSub;
    private XpBottleGiveAllSub xpBottleGiveAllSub;
    private CashNoteSub cashNoteSub;
    private CashNoteGiveAllSub cashNoteAllSub;

    private BTokensNoteSub bTokensNoteSub;
    private BTokensNoteAllSub bTokensNoteAllSub;

    public BeastWithdrawCMD(BeastWithdrawPlugin plugin) {
        pl = plugin;
        xpBottleGiveSub = new XpBottleGiveSub(pl, "BeastWithdraw.admin", 3, 5);
        xpBottleGiveAllSub = new XpBottleGiveAllSub(pl, "BeastWithdraw.admin", 2, 4);

        cashNoteSub = new CashNoteSub(pl, "BeastWithdraw.admin", 3, 5);
        cashNoteAllSub = new CashNoteGiveAllSub(pl, "BeastWithdraw.admin", 2, 4);

        bTokensNoteSub = new BTokensNoteSub(pl, "BeastWithdraw.admin", 3, 5);
        bTokensNoteAllSub = new BTokensNoteAllSub(pl, "BeastWithdraw.admin", 2, 4);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length >= 1) {

            switch (args[0].toLowerCase()) {

                case "givexpb":
                    //bw givexpb <player> <xp> <bottle-amount>
                    xpBottleGiveSub.run(sender, args);
                    break;
                case "givexpball":
                    xpBottleGiveAllSub.run(sender, args);
                    break;
                case "givexpbottle":
                    xpBottleGiveSub.run(sender, args);
                    break;
                case "givexpbottleall":
                    xpBottleGiveAllSub.run(sender, args);
                    break;
                case "givecashnote":
                    cashNoteSub.run(sender, args);
                    break;
                case "givecs":
                    cashNoteSub.run(sender, args);
                    break;
                case "givecashnoteall":
                    cashNoteAllSub.run(sender, args);
                    break;
                case "givecsall":
                    cashNoteAllSub.run(sender, args);
                    break;
                case "givetokennote":
                    bTokensNoteSub.run(sender, args);
                    break;
                case "givetokennoteall":
                    bTokensNoteAllSub.run(sender, args);
                    break;
                case "version":
                    pl.getUtils().sendMessage(sender, "&6========[&4Beast&bWithdraw&6]========");
                    pl.getUtils().sendMessage(sender, "&eAuthor: &fMrAxeTv");
                    pl.getUtils().sendMessage(sender, "&eVersion: &f" + pl.getDescription().getVersion());
                    pl.getUtils().sendMessage(sender, "&eDownload: &fwww.SpigotMC.org");
                    pl.getUtils().sendMessage(sender, "&6=============================");
                    break;
                case "reload":
                    if (sender.hasPermission("BeastWithdraw.admin")) {
                        pl.reload();
                        pl.getUtils().sendMessage(sender, "%prefix% &aConfig Reloaded!");
                        return true;
                    } else {
                        pl.getUtils().sendMessage(sender, "%prefix% &cYou need BeastWithdraw.admin permission!");
                    }

                default:
                    for (String s : pl.getMessages().getStringList("Help")) {
                        s = s.replaceAll("%player%", sender.getName());
                        pl.getUtils().sendMessage(sender, s);
                    }
                    break;
            }

            return true;

        }
        for (String s : pl.getMessages().getStringList("Help")) {
            s = s.replaceAll("%player%", sender.getName());
            pl.getUtils().sendMessage(sender, s);
        }

        return true;
    }

}
