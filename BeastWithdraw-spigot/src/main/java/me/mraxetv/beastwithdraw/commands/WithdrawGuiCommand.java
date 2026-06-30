package me.mraxetv.beastwithdraw.commands;

import me.mraxetv.beastlib.commands.builder.ShortCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.gui.withdraw.WithdrawGuiManager;
import me.mraxetv.beastwithdraw.gui.withdraw.WithdrawGuiProfile;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WithdrawGuiCommand extends ShortCommand {
    private final BeastWithdrawPlugin plugin;
    private final WithdrawGuiManager withdrawGuiManager;
    private final WithdrawGuiProfile profile;

    public WithdrawGuiCommand(BeastWithdrawPlugin plugin, WithdrawGuiManager withdrawGuiManager, WithdrawGuiProfile profile) {
        super(plugin, profile.getCommandName(), profile.getAliases(), profile.getPermission());
        this.plugin = plugin;
        this.withdrawGuiManager = withdrawGuiManager;
        this.profile = profile;
    }

    @Override
    public boolean execute(CommandSender sender, String cmd, String[] args) {
        if (!(sender instanceof Player)) {
            sendConfiguredMessage(sender, "WithdrawGui.OnlyPlayers", "%prefix% &cConsole can't open the withdraw GUI.");
            return true;
        }

        Player player = (Player) sender;
        if (!profile.isEnabled()) {
            sendConfiguredMessage(player, "WithdrawGui.Disabled", "%prefix% &cThe withdraw GUI is disabled.");
            return true;
        }

        withdrawGuiManager.open(player, profile);
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
