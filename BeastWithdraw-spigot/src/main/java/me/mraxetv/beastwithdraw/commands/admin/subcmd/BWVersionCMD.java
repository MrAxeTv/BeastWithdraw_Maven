package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastlib.commands.builder.CommandBuilder;
import me.mraxetv.beastlib.commands.builder.SubCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.command.CommandSender;

import java.util.List;

public class BWVersionCMD extends SubCommand {
    public BWVersionCMD(CommandBuilder handle, String name) {
        super(handle, name);
    }

    @Override
    public String getPermission() {
        return "BeastWithdraw.admin.version";
    }

    @Override
    public String getDescription() {
        return "This command will reload the plugin.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rl");
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    @Override
    public void playerExecute(CommandSender sender, String[] strings) {

        BeastWithdrawPlugin pl = BeastWithdrawPlugin.getInstance();
        pl.getUtils().sendMessage(sender, "&6========[&4Beast&bWithdraw&6]========");
        pl.getUtils().sendMessage(sender, "&eAuthor: &fMrAxeTv");
        pl.getUtils().sendMessage(sender, "&eVersion: &f" + pl.getDescription().getVersion());
        pl.getUtils().sendMessage(sender, "&eDownload: &fwww.SpigotMC.org");
        pl.getUtils().sendMessage(sender, "&6=============================");
    }

    @Override
    public void consoleExecute(CommandSender sender, String[] strings) {
        BeastWithdrawPlugin pl = BeastWithdrawPlugin.getInstance();
        pl.getUtils().sendMessage(sender, "&6========[&4Beast&bWithdraw&6]========");
        pl.getUtils().sendMessage(sender, "&eAuthor: &fMrAxeTv");
        pl.getUtils().sendMessage(sender, "&eVersion: &f" + pl.getDescription().getVersion());
        pl.getUtils().sendMessage(sender, "&eDownload: &fwww.SpigotMC.org");
        pl.getUtils().sendMessage(sender, "&6=============================");

    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
