package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastlib.commands.builder.CommandBuilder;
import me.mraxetv.beastlib.commands.builder.SubCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.admin.BeastWithdrawCMD;
import org.bukkit.command.CommandSender;

import java.util.List;

public class BWReloadCMD extends SubCommand {
    public BWReloadCMD(CommandBuilder handle, String name) {
        super(handle, name);
    }

    @Override
    public String getPermission() {
        return "BeastWithdraw.admin.reload";
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
        pl.reload();
        pl.getUtils().sendMessage(sender, "%prefix% &aConfig Reloaded!");
    }

    @Override
    public void consoleExecute(CommandSender sender, String[] strings) {
        BeastWithdrawPlugin pl = BeastWithdrawPlugin.getInstance();
        pl.reload();
        pl.getUtils().sendMessage(sender, "%prefix% &aConfig Reloaded!");
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
