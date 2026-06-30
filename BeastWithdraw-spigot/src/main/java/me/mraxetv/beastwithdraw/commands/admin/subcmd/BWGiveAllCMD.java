package me.mraxetv.beastwithdraw.commands.admin.subcmd;

import me.mraxetv.beastlib.commands.builder.CommandBuilder;
import me.mraxetv.beastlib.commands.builder.SubCommand;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.assets.BeastMcMMORedeemSkillHandler;
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
                            .replace("%prefix%", BeastWithdrawPlugin.getInstance().getUtils().getPrefix())
                            .replace("%type%", handlerID));
            return;
        }

        AssetHandler assetHandler = BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandler(handlerID);
        boolean skillNote = assetHandler instanceof BeastMcMMORedeemSkillHandler;
        String skillName = null;
        int amountIndex = 2;

        if (skillNote) {
            if (args.length < 4) {
                sendSkillUsage(sender);
                return;
            }

            BeastMcMMORedeemSkillHandler skillHandler = (BeastMcMMORedeemSkillHandler) assetHandler;
            if (!skillHandler.isSkillNotesEnabled()) {
                sendSkillDisabled(sender, skillHandler);
                return;
            }
            skillName = skillHandler.normalizeSkillName(args[2]);
            if (skillName == null || !skillHandler.isValidSkill(skillName)) {
                sendInvalidSkill(sender, skillHandler, args[2]);
                return;
            }
            amountIndex = 3;
        }

        if (!Utils.isDouble(args[amountIndex])) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                    BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.InvalidNumber")
                            .replace("%prefix%", BeastWithdrawPlugin.getInstance().getUtils().getPrefix())
                            .replace("%amount%", args[amountIndex]));
            return;
        }
        double amount = Double.parseDouble(args[amountIndex]);

        GiveOptions options = parseOptions(sender, args, amountIndex + 1, assetHandler);
        if (options == null) return;

        ItemStack item = skillNote
                ? ((BeastMcMMORedeemSkillHandler) assetHandler).getSkillItem(options.signer, skillName, amount,
                options.stackSize, options.signed, options.tax, options.amountOverrideId)
                : assetHandler.getItem(options.signer, amount, options.stackSize, options.signed, options.tax,
                options.amountOverrideId);

        for (Player player : Bukkit.getOnlinePlayers()) {
            Utils.addItem(player, item.clone()); // Clone to ensure separate item stacks
            if (!options.silent) {
                String message = assetHandler.getMessageSection().getString("RewardReceived");
                if (message == null || message.trim().isEmpty()) {
                    message = BeastWithdrawPlugin.getInstance().getUtils().getPrefix() + " &aYou received %stack%%amount%.";
                }
                message = message.replace("%prefix%", BeastWithdrawPlugin.getInstance().getUtils().getPrefix());
                message = message.replace("%amount%", assetHandler.formatWithPreSuffix(amount));
                message = message.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getBalanceAsDouble(player)));
                message = Utils.formatStackSize(message, options.stackSize);
                message = applyAssetPlaceholders(assetHandler, skillName, message, player);
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(player, message);
            }
        }

        String message = BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.Admin.Given")
                .replace("%amount%", assetHandler.formatWithPreSuffix(amount))
                .replace("%player%", "everyone")
                .replace("%type%", assetHandler.getID());
        message = Utils.formatStackSize(message, options.stackSize);
        message = applyAssetPlaceholders(assetHandler, skillName, message, null);
        BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, message);
    }

    private String applyAssetPlaceholders(AssetHandler assetHandler, String skillName, String message, Player target) {
        if (assetHandler instanceof BeastMcMMORedeemSkillHandler && skillName != null) {
            return ((BeastMcMMORedeemSkillHandler) assetHandler).applySkillPlaceholders(message, skillName, target);
        }
        return assetHandler.applyPlaceholders(message, target);
    }

    private GiveOptions parseOptions(CommandSender sender, String[] args, int startIndex, AssetHandler assetHandler) {
        int stackSize = 1;
        double tax = 0;
        String signer = "";
        boolean signerSet = false;
        boolean signed = false;
        boolean silent = false;
        String amountOverrideId = null;

        int index = startIndex;
        if (index < args.length && Utils.isInt(args[index])) {
            stackSize = Integer.parseInt(args[index]);
            if (stackSize <= 0) {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, "&cInvalid stack size.");
                return null;
            }
            index++;
        }

        if (index < args.length && Utils.isDouble(args[index])) {
            tax = Double.parseDouble(args[index]);
            if (tax < 0 || tax > 100) {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, "&cTax must be between 0 and 100.");
                return null;
            }
            index++;
        }

        for (int i = index; i < args.length; i++) {
            String arg = args[i];
            if (isSilentFlag(arg)) {
                silent = true;
                continue;
            }

            String inlineOverride = getInlineOverrideId(arg);
            if (inlineOverride != null) {
                amountOverrideId = inlineOverride;
                continue;
            }

            if (isOverrideFlag(arg)) {
                if (i + 1 >= args.length) {
                    sendUsage(sender);
                    return null;
                }
                amountOverrideId = args[++i];
                continue;
            }

            if (!signerSet) {
                signer = arg;
                signerSet = true;
                continue;
            }

            sendUsage(sender);
            return null;
        }

        if (amountOverrideId != null) {
            String canonical = assetHandler.getCanonicalAmountOverrideId(amountOverrideId);
            if (canonical == null) {
                sendUnknownOverride(sender, assetHandler, amountOverrideId);
                return null;
            }
            amountOverrideId = canonical;
        }

        return new GiveOptions(stackSize, tax, signer, signed, silent, amountOverrideId);
    }

    private void sendUsage(CommandSender sender) {
        BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender,
                BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.Admin.GiveAllUsage"));
    }

    private void sendSkillUsage(CommandSender sender) {
        String message = BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.Admin.SkillGiveAllUsage");
        if (message == null || message.trim().isEmpty()) {
            message = "%prefix% &cUse /BeastWithdraw giveAll mcmmoredeemskillcredits <skill> <amount> [stack] [tax] [signer] [-override <id>] [-s]";
        }
        BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, message);
    }

    private void sendSkillDisabled(CommandSender sender, BeastMcMMORedeemSkillHandler assetHandler) {
        String message = assetHandler.getSkillMessage("Disabled",
                "%prefix% &cWithdrawing mcMMO skill progress into notes is disabled.");
        message = assetHandler.applySkillPlaceholders(message, "", sender instanceof Player ? (Player) sender : null);
        BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, message);
    }

    private void sendInvalidSkill(CommandSender sender, BeastMcMMORedeemSkillHandler assetHandler, String skillInput) {
        String message = assetHandler.getSkillMessage("InvalidSkill", "%prefix% &cThat mcMMO skill is not available for skill notes.");
        message = message.replace("%skill%", skillInput == null ? "" : skillInput)
                .replace("%type%", skillInput == null ? "" : skillInput);
        BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, message);
    }

    private void sendUnknownOverride(CommandSender sender, AssetHandler assetHandler, String overrideId) {
        String message = BeastWithdrawPlugin.getInstance().getMessages().getString("Withdraws.Admin.UnknownAmountOverride");
        if (message == null || message.trim().isEmpty()) {
            message = "%prefix% &cUnknown amount override &b%override% &cfor &b%type%&c.";
        }
        message = message.replace("%prefix%", BeastWithdrawPlugin.getInstance().getUtils().getPrefix())
                .replace("%override%", overrideId == null ? "" : overrideId)
                .replace("%type%", assetHandler.getID());
        BeastWithdrawPlugin.getInstance().getUtils().sendMessage(sender, message);
    }

    private boolean isSilentFlag(String value) {
        return value != null && (value.equalsIgnoreCase("-s") || value.equalsIgnoreCase("-silent") || value.equalsIgnoreCase("silent"));
    }

    private boolean isOverrideFlag(String value) {
        return value != null && (value.equalsIgnoreCase("-override")
                || value.equalsIgnoreCase("-amountoverride")
                || value.equalsIgnoreCase("-amountOverride")
                || value.equalsIgnoreCase("-ao"));
    }

    private String getInlineOverrideId(String value) {
        if (value == null) return null;
        String lower = value.toLowerCase();
        if (lower.startsWith("override:") || lower.startsWith("override=")) return value.substring("override:".length()).trim();
        if (lower.startsWith("amountoverride:") || lower.startsWith("amountoverride=")) return value.substring("amountoverride:".length()).trim();
        return null;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 2:
                completions.addAll(BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandlerList());
                break;
            case 3:
                if (isSkillNoteHandler(args)) {
                    completions.addAll(getSkillCompletions(args));
                } else {
                    addAmountCompletions(completions);
                }
                break;
            case 4:
                if (isSkillNoteHandler(args)) {
                    addAmountCompletions(completions);
                } else {
                    addStackCompletions(completions);
                }
                break;
            case 5:
                if (isSkillNoteHandler(args)) {
                    addStackCompletions(completions);
                } else {
                    addTaxCompletions(completions);
                }
                break;
            case 6:
                if (isSkillNoteHandler(args)) {
                    addTaxCompletions(completions);
                } else {
                    addSignerAndFlagCompletions(completions);
                }
                break;
            case 7:
                if (isSkillNoteHandler(args)) {
                    addSignerAndFlagCompletions(completions);
                    break;
                }
                completions.addAll(getAmountOverrideCompletions(args));
                completions.add("-override");
                completions.add("-ao");
                completions.add("-s");
                completions.add("-silent");
                break;
            case 8:
                if (isSkillNoteHandler(args)) {
                    completions.addAll(getAmountOverrideCompletions(args));
                    completions.add("-override");
                    completions.add("-ao");
                    completions.add("-s");
                    completions.add("-silent");
                }
                break;
        }
        if (args.length >= 4 && isPreviousOverrideFlag(args)) {
            return getAmountOverrideCompletions(args);
        }
        return completions;
    }

    private void addAmountCompletions(List<String> completions) {
        completions.add("100");
        completions.add("500");
        completions.add("1000");
    }

    private void addStackCompletions(List<String> completions) {
        completions.add("1");
        completions.add("16");
        completions.add("64");
    }

    private void addTaxCompletions(List<String> completions) {
        completions.add("0");
        completions.add("10");
        completions.add("50");
    }

    private void addSignerAndFlagCompletions(List<String> completions) {
        completions.add("AdminNote");
        completions.add("EventReward");
        completions.add("-override");
        completions.add("-ao");
        completions.add("-s");
        completions.add("-silent");
    }

    private boolean isPreviousOverrideFlag(String[] args) {
        return args.length >= 2 && isOverrideFlag(args[args.length - 2]);
    }

    private boolean isSkillNoteHandler(String[] args) {
        if (args.length < 2) return false;
        AssetHandler assetHandler = BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandler(args[1]);
        return assetHandler instanceof BeastMcMMORedeemSkillHandler
                && ((BeastMcMMORedeemSkillHandler) assetHandler).isSkillNotesEnabled();
    }

    private List<String> getSkillCompletions(String[] args) {
        List<String> values = new ArrayList<>();
        if (args.length < 2) return values;
        AssetHandler assetHandler = BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandler(args[1]);
        if (!(assetHandler instanceof BeastMcMMORedeemSkillHandler)) return values;
        BeastMcMMORedeemSkillHandler skillHandler = (BeastMcMMORedeemSkillHandler) assetHandler;
        if (!skillHandler.isSkillNotesEnabled()) return values;
        values.addAll(skillHandler.getSkillSuggestions());
        return values;
    }

    private List<String> getAmountOverrideCompletions(String[] args) {
        List<String> values = new ArrayList<>();
        if (args.length < 2) return values;
        AssetHandler assetHandler = BeastWithdrawPlugin.getInstance().getWithdrawManager().getAssetHandler(args[1]);
        if (assetHandler == null) return values;
        values.addAll(assetHandler.getAmountOverrideIds());
        return values;
    }

    private static class GiveOptions {
        private final int stackSize;
        private final double tax;
        private final String signer;
        private final boolean signed;
        private final boolean silent;
        private final String amountOverrideId;

        private GiveOptions(int stackSize, double tax, String signer, boolean signed, boolean silent, String amountOverrideId) {
            this.stackSize = stackSize;
            this.tax = tax;
            this.signer = signer;
            this.signed = signed;
            this.silent = silent;
            this.amountOverrideId = amountOverrideId;
        }
    }
}
