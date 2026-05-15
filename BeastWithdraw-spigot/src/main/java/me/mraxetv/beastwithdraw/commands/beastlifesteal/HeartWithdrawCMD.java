package me.mraxetv.beastwithdraw.commands.beastlifesteal;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.managers.assets.BeastLifeStealHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HeartWithdrawCMD extends WithdrawCMD {

    private final BeastWithdrawPlugin plugin;
    private final BeastLifeStealHandler assetHandler;
    private boolean confirmRequested;

    public HeartWithdrawCMD(BeastWithdrawPlugin plugin, BeastLifeStealHandler assetHandler) {
        super(plugin, assetHandler);
        this.plugin = plugin;
        this.assetHandler = assetHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String cmd, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getUtils().sendMessage(sender, assetHandler.getMessageSection().getString("PlayerOnly"));
            return true;
        }

        Player player = (Player) sender;
        if (!hasWithdrawPermission(player)) {
            plugin.getUtils().sendMessage(player, assetHandler.getMessageSection().getString("NoPermission"));
            return true;
        }

        if (!assetHandler.isAvailable()) {
            plugin.getUtils().sendMessage(player, assetHandler.getMessageSection().getString("MissingDependency"));
            return true;
        }

        confirmRequested = false;
        String[] processedArgs = preprocessArgs(player, args);
        if (processedArgs == null) {
            return true;
        }

        return super.execute(sender, cmd, processedArgs);
    }

    @Override
    public boolean testPermission(CommandSender target) {
        return true;
    }

    @Override
    protected void sendHelpMessage(Player p) {
        String helpMessage = assetHandler.getMessageSection().getString("Help");
        helpMessage = helpMessage.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalance(p)));
        plugin.getUtils().sendMessage(p, helpMessage);
    }

    @Override
    protected double parseWithdrawAmount(Player p, String arg, double balance) {
        if (!Utils.isDouble(arg)) {
            String message = assetHandler.getMessageSection().getString("InvalidAmount");
            plugin.getUtils().sendMessage(p, message.replace("%amount%", arg));
            return -1;
        }

        double amount = super.parseWithdrawAmount(p, arg, balance);
        if (amount <= 0) {
            String message = assetHandler.getMessageSection().getString("InvalidAmount");
            plugin.getUtils().sendMessage(p, message.replace("%amount%", arg));
            return -1;
        }
        return amount;
    }

    @Override
    protected int parseStackSize(Player p, String[] args) {
        if (args.length < 2) {
            return 1;
        }

        if (!Utils.isInt(args[1])) {
            String message = assetHandler.getMessageSection().getString("InvalidAmount");
            plugin.getUtils().sendMessage(p, message.replace("%amount%", args[1]));
            return -1;
        }

        return super.parseStackSize(p, new String[]{args[0], args[1]});
    }

    @Override
    protected boolean validateWithdrawLimits(Player p, double takenAmount) {
        if (!super.validateWithdrawLimits(p, takenAmount)) {
            return false;
        }

        if (assetHandler.getConfig().getBoolean("Settings.BlockDuringGracePeriod", true) && assetHandler.isGracePeriodActive()) {
            plugin.getUtils().sendMessage(p, assetHandler.getMessageSection().getString("GracePeriodBlocked"));
            return false;
        }

        return true;
    }

    @Override
    protected boolean validateBalance(Player p, double balance, double takenAmount, int stackSize) {
        if (!super.validateBalance(p, balance, takenAmount, stackSize)) {
            return false;
        }

        double totalHearts = takenAmount * stackSize;
        if (!assetHandler.wouldWithdrawEliminate(p, (int) totalHearts)) {
            return true;
        }

        if (!assetHandler.getConfig().getBoolean("Settings.AllowLethalWithdraw", false)) {
            plugin.getUtils().sendMessage(p, assetHandler.getMessageSection().getString("LethalBlocked"));
            return false;
        }

        if (assetHandler.getConfig().getBoolean("Settings.RequireConfirmOnLethal", true) && !confirmRequested) {
            String usage = stackSize > 1
                    ? "/heartwithdraw " + Utils.formatDouble(takenAmount) + " " + stackSize + " confirm"
                    : "/heartwithdraw " + Utils.formatDouble(takenAmount) + " confirm";
            String message = assetHandler.getMessageSection().getString("ConfirmRequired");
            message = message.replace("%amount%", assetHandler.formatWithPreSuffix(totalHearts));
            message = message.replace("%usage%", usage);
            plugin.getUtils().sendMessage(p, message);
            return false;
        }

        return true;
    }

    private boolean hasWithdrawPermission(Player player) {
        String permission = assetHandler.getConfig().getString("Settings.Permission", "beastlifesteal.withdraw");
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

    private String[] preprocessArgs(Player player, String[] args) {
        if (args.length == 0) {
            return new String[]{String.valueOf(Math.max(1, assetHandler.getConfig().getInt("Settings.DefaultAmount", 1)))};
        }

        if (args.length == 1 && "confirm".equalsIgnoreCase(args[0])) {
            confirmRequested = true;
            return new String[]{String.valueOf(Math.max(1, assetHandler.getConfig().getInt("Settings.DefaultAmount", 1)))};
        }

        if (args.length > 3) {
            sendInvalidAmount(player, args[3]);
            return null;
        }

        if (args.length == 3) {
            if (!"confirm".equalsIgnoreCase(args[2])) {
                sendInvalidAmount(player, args[2]);
                return null;
            }
            confirmRequested = true;
            return new String[]{args[0], args[1]};
        }

        if (args.length == 2) {
            if ("confirm".equalsIgnoreCase(args[1])) {
                confirmRequested = true;
                return new String[]{args[0]};
            }
        }

        return args;
    }

    private void sendInvalidAmount(Player player, String input) {
        String message = assetHandler.getMessageSection().getString("InvalidAmount");
        plugin.getUtils().sendMessage(player, message.replace("%amount%", input));
    }
}
