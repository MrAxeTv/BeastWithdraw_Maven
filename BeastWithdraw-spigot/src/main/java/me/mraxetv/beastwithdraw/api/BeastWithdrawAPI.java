package me.mraxetv.beastwithdraw.api;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.tokenwithdraw.BeastTokenNoteCMD;
import me.mraxetv.beastwithdraw.managers.WithdrawManager;
import me.mraxetv.beastwithdraw.managers.assets.BeastTokensHandler;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public final class BeastWithdrawAPI {

    private BeastWithdrawAPI() {
    }

    public static boolean handleBeastTokensWithdraw(CommandSender sender, String currencyId, String[] beastTokensArgs) {
        BeastTokensHandler handler = getBeastTokensHandler(currencyId);
        if (handler == null) return false;

        BeastTokenNoteCMD command = handler.getBeastTokenNoteCMD();
        if (command == null) return false;

        command.execute(sender, handler.getCommandName(), stripBeastTokensSubCommand(beastTokensArgs));
        return true;
    }

    public static List<String> tabCompleteBeastTokensWithdraw(CommandSender sender, String currencyId, String[] beastTokensArgs) {
        BeastTokensHandler handler = getBeastTokensHandler(currencyId);
        if (handler == null || handler.getBeastTokenNoteCMD() == null) return null;

        List<String> completions = handler.getBeastTokenNoteCMD().tabComplete(sender, handler.getCommandName(), stripBeastTokensSubCommand(beastTokensArgs));
        return completions == null ? Collections.<String>emptyList() : completions;
    }

    public static boolean refreshBeastTokensCurrencies() {
        BeastWithdrawPlugin plugin = BeastWithdrawPlugin.getInstance();
        if (plugin == null || plugin.getWithdrawManager() == null) return false;

        plugin.getWithdrawManager().refreshBeastTokensAssets();
        return true;
    }

    private static BeastTokensHandler getBeastTokensHandler(String currencyId) {
        BeastWithdrawPlugin plugin = BeastWithdrawPlugin.getInstance();
        if (plugin == null || plugin.getWithdrawManager() == null) return null;

        WithdrawManager withdrawManager = plugin.getWithdrawManager();
        return withdrawManager.getBeastTokensHandler(currencyId);
    }

    private static String[] stripBeastTokensSubCommand(String[] args) {
        if (args == null || args.length == 0) return new String[0];
        if (!args[0].equalsIgnoreCase("withdraw") && !args[0].equalsIgnoreCase("note")) return args;

        String[] stripped = new String[args.length - 1];
        System.arraycopy(args, 1, stripped, 0, stripped.length);
        return stripped;
    }
}
