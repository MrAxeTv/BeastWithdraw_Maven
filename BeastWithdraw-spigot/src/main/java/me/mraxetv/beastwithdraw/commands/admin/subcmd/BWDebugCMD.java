package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastlib.commands.builder.CommandBuilder;
import me.mraxetv.beastlib.commands.builder.SubCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BWDebugCMD extends SubCommand {

    public BWDebugCMD(CommandBuilder handle, String name) {
        super(handle, name);
    }

    @Override
    public String getPermission() {
        return "BeastWithdraw.admin.debug";
    }

    @Override
    public String getDescription() {
        return "Toggle transaction log output in console for the current session.";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("dbg");
    }

    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public void playerExecute(CommandSender sender, String[] strings) {
        execute(sender, strings);
    }

    @Override
    public void consoleExecute(CommandSender sender, String[] strings) {
        execute(sender, strings);
    }

    private void execute(CommandSender sender, String[] args) {
        BeastWithdrawPlugin plugin = BeastWithdrawPlugin.getInstance();

        if (args.length <= 1) {
            boolean nextState = !plugin.getWithdrawLogger().isConsoleLoggingEnabled();
            plugin.getWithdrawLogger().setConsoleLoggingOverride(nextState);
            sendStateMessage(sender, plugin, nextState, true);
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("on")) {
            plugin.getWithdrawLogger().setConsoleLoggingOverride(true);
            sendStateMessage(sender, plugin, true, true);
            return;
        }

        if (action.equals("off")) {
            plugin.getWithdrawLogger().setConsoleLoggingOverride(false);
            sendStateMessage(sender, plugin, false, true);
            return;
        }

        if (action.equals("reset") || action.equals("default")) {
            plugin.getWithdrawLogger().setConsoleLoggingOverride(null);
            boolean currentState = plugin.getWithdrawLogger().isConsoleLoggingEnabled();
            plugin.getUtils().sendMessage(sender,
                    "%prefix% &eWithdraw log console output now follows config.yml: &f"
                            + (currentState ? "&aON" : "&cOFF")
                            + "&e.");
            return;
        }

        if (action.equals("status")) {
            boolean currentState = plugin.getWithdrawLogger().isConsoleLoggingEnabled();
            boolean override = plugin.getWithdrawLogger().hasConsoleLoggingOverride();
            plugin.getUtils().sendMessage(sender,
                    "%prefix% &eWithdraw log console output: &f"
                            + (currentState ? "&aON" : "&cOFF")
                            + "&e. Config: &f"
                            + (plugin.getWithdrawLogger().isConsoleLoggingEnabledInConfig() ? "&aON" : "&cOFF")
                            + "&e. Session override: &f"
                            + (override ? "&6ACTIVE" : "&7NONE"));
            return;
        }

        plugin.getUtils().sendMessage(sender, "%prefix% &cUsage: /beastwithdraw debug [on|off|reset|status]");
    }

    private void sendStateMessage(CommandSender sender, BeastWithdrawPlugin plugin, boolean enabled, boolean temporary) {
        String suffix = temporary ? " &7(Session only until restart/reload)" : "";
        plugin.getUtils().sendMessage(sender,
                "%prefix% &eWithdraw log console output: &f" + (enabled ? "&aON" : "&cOFF") + suffix);
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return Arrays.asList("on", "off", "reset", "status");
        }
        return Collections.emptyList();
    }
}
