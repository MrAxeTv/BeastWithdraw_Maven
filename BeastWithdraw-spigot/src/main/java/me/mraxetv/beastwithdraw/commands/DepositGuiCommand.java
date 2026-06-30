package me.mraxetv.beastwithdraw.commands;

import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.gui.depositor.DepositGuiManager;
import me.mraxetv.beastwithdraw.gui.depositor.DepositGuiProfile;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DepositGuiCommand extends ShortCommand {
    private final BeastWithdrawPlugin plugin;
    private final DepositGuiManager depositGuiManager;
    private final DepositGuiProfile profile;

    public DepositGuiCommand(BeastWithdrawPlugin plugin, DepositGuiManager depositGuiManager, DepositGuiProfile profile) {
        super(plugin, profile.getCommandName(), profile.getAliases(), profile.getPermission());
        this.plugin = plugin;
        this.depositGuiManager = depositGuiManager;
        this.profile = profile;
    }

    @Override
    public boolean execute(CommandSender sender, String cmd, String[] args) {
        if (!(sender instanceof Player)) {
            sendConfiguredMessage(sender, "Depositor.OnlyPlayers", "%prefix% &cConsole can't open the depositor GUI.");
            return true;
        }

        Player player = (Player) sender;
        if (!profile.isEnabled()) {
            sendConfiguredMessage(player, "Depositor.Disabled", "%prefix% &cThe depositor GUI is disabled.");
            return true;
        }

        depositGuiManager.open(player, profile);
        return true;
    }

    @Override
    public boolean testPermission(CommandSender target) {
        String permission = getPermission();
        if (permission == null || permission.trim().isEmpty() || target.hasPermission(permission)) {
            return true;
        }

        if (target instanceof Player) {
            plugin.getUtils().noPermission((Player) target);
        } else {
            sendConfiguredMessage(target, "Withdraws.NoPermission", "%prefix% &4You don't have permission to do that.");
        }
        return false;
    }

    private void sendConfiguredMessage(CommandSender sender, String path, String fallback) {
        String message = plugin.getMessages().contains(path) ? plugin.getMessages().getString(path) : fallback;
        plugin.getUtils().sendMessage(sender, message);
    }

}
