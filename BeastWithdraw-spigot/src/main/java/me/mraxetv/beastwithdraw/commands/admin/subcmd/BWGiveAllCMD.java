package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastlib.commands.builder.CommandBuilder;
import me.mraxetv.beastlib.commands.builder.SubCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BWGiveAllCMD extends SubCommand {

    public BWGiveAllCMD(CommandBuilder handle, String name) {
        super(handle, name); // "giveall"
    }

    @Override
    public String getPermission() {
        return "BeastWithdraw.admin.giveall";
    }

    @Override
    public String getDescription() {
        return "Give all online players a note.";
    }

    @Override
    public List<String> getAliases() {
        return new ArrayList<>();
    }

    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public void playerExecute(CommandSender sender, String[] args) {
        execute(sender, args);
    }

    @Override
    public void consoleExecute(CommandSender sender, String[] args) {
        execute(sender, args);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                    BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.Admin.GiveAllUsage"));
            return;
        }

        String handlerID = args[1];
        if (!BeastWithdrawPlugin.getInstance().getWithdrawManager().hasAssetHandler(handlerID)) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                    BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.WrongTypeName")
                            .replace("%prefix%", Utils.getPrefix())
                            .replace("%type%", handlerID));
            return;
        }

        if (!Utils.isDouble(args[2])) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                    BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.InvalidNumber")
                            .replace("%prefix%", Utils.getPrefix())
                            .replace("%amount%", args[1]));
            return;
        }
        double amount = Double.parseDouble(args[2]);

        int stackSize = 1;
        if (args.length > 3) {
            if (!Utils.isInt(args[3])) {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, "&cInvalid stack size.");
                return;
            }
            stackSize = Integer.parseInt(args[3]);
        }

        double tax = 0;
        if (args.length > 4) {
            if (!Utils.isDouble(args[4])) {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, "&cInvalid tax value.");
                return;
            }
            tax = Double.parseDouble(args[4]);
            if (tax < 0 || tax > 100) {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, "&cTax must be between 0 and 100.");
                return;
            }
        }

        // Optional signer
        String signer = "";
        boolean signet = false;

        // Optional silent flag
        boolean silent = false;

        if (args.length >= 6) {
            String arg = args[5];
            if (arg.equalsIgnoreCase("-s") || arg.equalsIgnoreCase("-silent")) {
                silent = true;
            } else {
                signer = arg;
                signet = true;
            }
        }

        if (args.length == 6) {
            String arg = args[5];
            if (arg.equalsIgnoreCase("-s") || arg.equalsIgnoreCase("-silent")) {
                silent = true;
            }
        }

        AssetHandler assetHandler = BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandler(handlerID);
        ItemStack item = assetHandler.getItem(signer, amount, stackSize, signet, tax);

        for (Player player : Bukkit.getOnlinePlayers()) {
            Utils.addItem(player, item.clone()); // Clone to ensure separate item stacks
            if (!silent) {
                String message = assetHandler.getMessageSection().getString("RewardReceived");
                message = message.replace("%amount%", assetHandler.formatWithPreSuffix(amount));
                message = Utils.formatStackSize(message, stackSize);
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(player, message);
            }
        }

        String message = BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.Admin.Given")
                .replace("%amount%", assetHandler.formatWithPreSuffix(amount))
                .replace("%player%", "everyone")
                .replace("%type%", handlerID);
        message = Utils.formatStackSize(message, stackSize);
        BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, message);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 2:
                completions.addAll(BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandlerList());
                break;
            case 3:
                completions.add("100");
                completions.add("500");
                completions.add("1000");
                break;
            case 4:
                completions.add("1");
                completions.add("16");
                completions.add("64");
                break;
            case 5:
                completions.add("0");
                completions.add("10");
                completions.add("50");
                break;
            case 6:
                completions.add("AdminNote");
                completions.add("EventReward");
                completions.add("-s");
                completions.add("-silent");
                break;
            case 7:
                completions.add("-s");
                completions.add("-silent");
                break;
        }
        return completions;
    }
}
