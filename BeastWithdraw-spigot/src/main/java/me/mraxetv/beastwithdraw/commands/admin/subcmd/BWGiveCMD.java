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

public class BWGiveCMD extends SubCommand {

    public BWGiveCMD(CommandBuilder handle, String name) {
        super(handle, name);
    }

    @Override
    public String getPermission() {
        return "BeastWithdraw.admin.give";
    }

    @Override
    public String getDescription() {
        return "Give player a note.";
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
        if (args.length < 4) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                    BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.Admin.GiveUsage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                    BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.NotOnline")
                            .replace("%prefix%", Utils.getPrefix())
                            .replace("%player%", args[1]));
            return;
        }

        String handlerID = args[2];
        if (!BeastWithdrawPlugin.getInstance().getWithdrawManager().hasAssetHandler(handlerID)) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                    BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.WrongTypeName")
                            .replace("%prefix%", Utils.getPrefix())
                            .replace("%type%", handlerID)
                            .replace("%player%", args[1]));
            return;
        }

        if (!Utils.isDouble(args[3])) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                    BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.InvalidNumber")
                            .replace("%prefix%", Utils.getPrefix())
                            .replace("%amount%", args[3]));
            return;
        }
        double amount = Double.parseDouble(args[3]);

        int stackSize = 1;
        if(args.length > 4) {
            if (!Utils.isInt(args[4])) {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, "&cInvalid stack size.");
                return;
            }
            stackSize = Integer.parseInt(args[4]);
        }

        double tax = 0;
        if(args.length > 5){
        if (!Utils.isDouble(args[5])) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, "&cInvalid tax value.");
            return;
        }
        tax = Double.parseDouble(args[5]);
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

        if (args.length >= 7) {
            String arg6 = args[6];
            if (arg6.equalsIgnoreCase("-s") || arg6.equalsIgnoreCase("-silent")) {
                silent = true;
            } else {
                signer = arg6;
                signet = true;
            }
        }

        if (args.length == 8) {
            String arg7 = args[7];
            if (arg7.equalsIgnoreCase("-s") || arg7.equalsIgnoreCase("-silent")) {
                silent = true;
            }
        }

        AssetHandler assetHandler = BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandler(handlerID);
        ItemStack item = assetHandler.getItem(signer, amount, stackSize, signet, tax);
        Utils.addItem(target, item);

        if (!silent) {
            String message = assetHandler.getMessageSection().getString("RewardReceived");
            message = message.replace("%amount%", assetHandler.formatWithPreSuffix(amount));
            message = Utils.formatStackSize(message, stackSize);
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(target, message);
        }

        String message = BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.Admin.Given")
                .replace("%amount%", assetHandler.formatWithPreSuffix(amount))
                .replace("%player%", target.getName())
                .replace("%type%", handlerID);
        message = Utils.formatStackSize(message, stackSize);
        BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, message);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 2:
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
                break;

            case 3:
                completions.addAll(BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandlerList());
                break;

            case 4:
                completions.add("100");
                completions.add("500");
                completions.add("1000");
                completions.add("100000");
                completions.add("1000000");
                break;

            case 5:
                completions.add("1");
                completions.add("16");
                completions.add("32");
                completions.add("64");
                break;

            case 6:
                completions.add("0");
                completions.add("5");
                completions.add("10");
                completions.add("20");
                completions.add("50");
                completions.add("100");
                break;

            case 7:
                completions.add("AdminNote");
                completions.add("Reward");
                completions.add("EventDrop");
                completions.add("-s");
                completions.add("-silent");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName()); // could be used as signer
                }
                break;

            case 8:
                completions.add("-s");
                completions.add("-silent");
                break;
        }

        return completions;
    }
}
