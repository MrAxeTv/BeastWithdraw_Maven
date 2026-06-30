package me.mraxetv.beastwithdraw.commands.mcmmoredeem;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.managers.WithdrawItemRequirement;
import me.mraxetv.beastwithdraw.managers.assets.BeastMcMMORedeemHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class McMMORedeemCreditNoteCMD extends WithdrawCMD {
    private final BeastWithdrawPlugin plugin;
    private final BeastMcMMORedeemHandler assetHandler;

    public McMMORedeemCreditNoteCMD(BeastWithdrawPlugin plugin, BeastMcMMORedeemHandler assetHandler) {
        super(plugin, assetHandler);
        this.plugin = plugin;
        this.assetHandler = assetHandler;
    }

    @Override
    public boolean execute(CommandSender sender, String cmd, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getUtils().sendMessage(sender, "%prefix% Console can't use this command! /BeastWithdraw help");
            return true;
        }

        Player player = (Player) sender;
        if (isSkillCommandInput(args)) {
            if (!assetHandler.isSkillNotesEnabled()) {
                sendSkillMessage(player, "Disabled",
                        "%prefix% &cWithdrawing mcMMO skill progress into notes is disabled.", args[0]);
                return true;
            }
            withdrawSkill(player, args);
            return true;
        }

        return super.execute(sender, cmd, args);
    }

    private boolean isSkillCommandInput(String[] args) {
        if (args == null || args.length < 2) {
            return false;
        }

        return assetHandler.normalizeSkillName(args[0]) != null;
    }

    private void withdrawSkill(Player player, String[] args) {
        if (args == null || args.length < 2) {
            sendHelpMessage(player);
            return;
        }

        int stackSize = parseSkillStackSize(player, args);
        if (stackSize == -1) {
            return;
        }

        withdrawSkill(player, args[0], args[1], stackSize, null);
    }

    public boolean withdrawSkill(Player player, String skillInput, String amountInput, int stackSize,
                                 WithdrawItemRequirement.ItemSource itemSource) {
        if (!assetHandler.isSkillNotesEnabled()) {
            sendSkillMessage(player, "Disabled",
                    "%prefix% &cWithdrawing mcMMO skill progress into notes is disabled.", skillInput);
            return false;
        }

        if (!assetHandler.hasSkillWithdrawPermission(player)) {
            plugin.getUtils().noPermission(player);
            return false;
        }

        String skillName = assetHandler.normalizeSkillName(skillInput);
        if (skillName == null || !assetHandler.isValidSkill(skillName)) {
            sendSkillMessage(player, "InvalidSkill", "%prefix% &cThat mcMMO skill is not available.", skillName);
            return false;
        }
        stackSize = normalizeSkillStackSize(player, stackSize);

        double balance = assetHandler.getSkillBalance(player, skillName);
        double takenAmount = parseSkillAmount(player, amountInput, balance, skillName);
        if (takenAmount == -1D) {
            return false;
        }

        if (!validateInventorySpace(player)) return false;
        if (!validateSkillWithdrawLimits(player, takenAmount, skillName)) return false;
        if (!validateSkillBigAmount(player, takenAmount, stackSize, skillName)) return false;
        if (!validateSkillBalance(player, balance, takenAmount, stackSize, skillName)) return false;

        WithdrawItemRequirement.ConsumeResult requiredItem = consumeSkillRequiredItem(player, stackSize, itemSource);
        if (!requiredItem.isSuccess()) return false;

        double chargedFee = chargeSkillFee(player, stackSize, skillName);
        if (chargedFee < 0D) {
            requiredItem.rollback(player);
            return false;
        }

        return performSkillWithdraw(player, skillName, takenAmount, stackSize, requiredItem, chargedFee);
    }

    private double parseSkillAmount(Player player, String rawAmount, double balance, String skillName) {
        if (rawAmount.equalsIgnoreCase("all")) {
            if (!skillHandler().hasWithdrawAllPermission(player)) {
                plugin.getUtils().noPermission(player);
                return -1D;
            }
            return Math.floor(balance);
        }

        if (!Utils.isDouble(rawAmount)) {
            String message = plugin.getMessages().getString("Withdraws.InvalidNumber");
            message = message.replace("%amount%", rawAmount);
            plugin.getUtils().sendMessage(player, message);
            return -1D;
        }

        BigDecimal amount = new BigDecimal(rawAmount).setScale(0, RoundingMode.DOWN);
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            sendSkillMessage(player, "InvalidAmount", "%prefix% &cEnter a positive whole number.", skillName);
            return -1D;
        }
        return amount.doubleValue();
    }

    private int parseSkillStackSize(Player player, String[] args) {
        if (args.length < 3) {
            return 1;
        }

        if (!Utils.isInt(args[2])) {
            String message = plugin.getMessages().getString("Withdraws.InvalidNumber");
            message = message.replace("%amount%", args[2]);
            plugin.getUtils().sendMessage(player, message);
            return -1;
        }

        int stackSize = Math.abs(Integer.parseInt(args[2]));
        int maxStack = skillHandler().getConfig().getInt("Settings.MaxStackSize", 64);
        if (stackSize > maxStack) {
            String message = skillHandler().getMessageSection().getString("MaxStackSize");
            message = message.replace("%stack%", Utils.formatNumber(maxStack));
            message = skillHandler().applySkillPlaceholders(message, "", player);
            plugin.getUtils().sendMessage(player, message);
            return maxStack;
        }

        return Math.max(stackSize, 1);
    }

    private int normalizeSkillStackSize(Player player, int stackSize) {
        int normalized = Math.max(stackSize, 1);
        int maxStack = skillHandler().getConfig().getInt("Settings.MaxStackSize", 64);
        if (normalized > maxStack) {
            String message = skillHandler().getMessageSection().getString("MaxStackSize");
            message = message.replace("%stack%", Utils.formatNumber(maxStack));
            message = skillHandler().applySkillPlaceholders(message, "", player);
            plugin.getUtils().sendMessage(player, message);
            return Math.max(maxStack, 1);
        }
        return normalized;
    }

    private boolean validateSkillWithdrawLimits(Player player, double takenAmount, String skillName) {
        double min = getSkillPermissionMin(player);
        double max = getSkillPermissionMax(player);

        if (takenAmount <= 0 || takenAmount < min) {
            String message = skillHandler().getMessageSection().getString("Min");
            message = message.replace("%min-amount%", skillHandler().formatNumber(min));
            message = skillHandler().applySkillPlaceholders(message, skillName, player);
            plugin.getUtils().sendMessage(player, message);
            return false;
        }

        if (takenAmount > max) {
            String message = skillHandler().getMessageSection().getString("Max");
            message = message.replace("%max-amount%", skillHandler().formatNumber(max));
            message = skillHandler().applySkillPlaceholders(message, skillName, player);
            plugin.getUtils().sendMessage(player, message);
            return false;
        }

        return true;
    }

    private double getSkillPermissionMin(Player player) {
        double min = skillHandler().getConfig().getDouble("Settings.Min");
        if (skillHandler().getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            for (String key : skillHandler().getConfig().getSection("Settings.PermissionNotes").getRoutesAsStrings(false)) {
                if (skillHandler().hasPermissionNote(player, key)) {
                    min = skillHandler().getConfig().getDouble("Settings.PermissionNotes." + key + ".Min");
                }
            }
        }
        return min;
    }

    private double getSkillPermissionMax(Player player) {
        double max = skillHandler().getConfig().getDouble("Settings.Max");
        if (skillHandler().getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            for (String key : skillHandler().getConfig().getSection("Settings.PermissionNotes").getRoutesAsStrings(false)) {
                if (skillHandler().hasPermissionNote(player, key)) {
                    max = skillHandler().getConfig().getDouble("Settings.PermissionNotes." + key + ".Max");
                }
            }
        }
        return max;
    }

    private boolean validateSkillBigAmount(Player player, double takenAmount, int stackSize, String skillName) {
        if (!skillHandler().isToBigAmount(takenAmount * stackSize)) {
            return true;
        }

        String message = skillHandler().getMessageSection().getString("ToBigNumber");
        message = message.replace("%amount%", skillHandler().formatWithPreSuffix(takenAmount * stackSize));
        message = skillHandler().applySkillPlaceholders(message, skillName, player);
        plugin.getUtils().sendMessage(player, message);
        return false;
    }

    private boolean validateSkillBalance(Player player, double balance, double takenAmount, int stackSize, String skillName) {
        if (balance >= takenAmount * stackSize) {
            return true;
        }

        String message = assetHandler.getSkillMessage("NotEnough",
                "%prefix% &cYou do not have %amount% stored in %type%. &7Available: &b%balance%&7.");
        message = message.replace("%balance%", assetHandler.formatWithPreSuffix(balance));
        message = message.replace("%amount%", assetHandler.formatWithPreSuffix(takenAmount * stackSize));
        message = assetHandler.applySkillPlaceholders(message, skillName, player);
        plugin.getUtils().sendMessage(player, message);
        return false;
    }

    private WithdrawItemRequirement.ConsumeResult consumeSkillRequiredItem(Player player, int stackSize,
                                                                          WithdrawItemRequirement.ItemSource itemSource) {
        WithdrawItemRequirement.ItemSource source = itemSource == null
                ? WithdrawItemRequirement.playerInventory(player)
                : itemSource;
        return skillHandler().getWithdrawItemRequirement().consume(
                player,
                source,
                stackSize
        );
    }

    private double chargeSkillFee(Player player, int stackSize, String skillName) {
        if (skillHandler().hasBypassFeePermission(player)) {
            return 0D;
        }
        if (!skillHandler().getConfig().getBoolean("Settings.Charges.Fee.Enabled")) {
            return 0D;
        }

        double fee = skillHandler().getConfig().getDouble("Settings.Charges.Fee.Cost") * stackSize;
        if (fee <= 0D) {
            return 0D;
        }

        double balance = assetHandler.getSkillBalance(player, skillName);
        if (balance < fee) {
            String message = skillHandler().getMessageSection().getString("Fee.NotEnough");
            if (message == null) message = skillHandler().getMessageSection().getString("Tax.NotEnough");
            message = message.replace("%amount%", skillHandler().formatWithPreSuffix(fee));
            message = message.replace("%fee%", skillHandler().formatWithPreSuffix(fee));
            message = skillHandler().applySkillPlaceholders(message, skillName, player);
            plugin.getUtils().sendMessage(player, message);
            return -1D;
        }

        if (!assetHandler.withdrawSkillAmount(player, skillName, (long) Math.floor(fee))) {
            sendSkillMessage(player, "TransactionFailed",
                    "%prefix% &cThe mcMMO skill note transaction could not be completed right now.", skillName);
            return -1D;
        }

        String message = skillHandler().getMessageSection().getString("Fee.TakenFee");
        if (message == null) message = skillHandler().getMessageSection().getString("Tax.TakenFee");
        message = message.replace("%fee%", skillHandler().formatWithPreSuffix(fee));
        message = skillHandler().applySkillPlaceholders(message, skillName, player);
        plugin.getUtils().sendMessage(player, message);
        return fee;
    }

    private boolean performSkillWithdraw(Player player, String skillName, double takenAmount, int stackSize,
                                         WithdrawItemRequirement.ConsumeResult requiredItem, double chargedFee) {
        long totalAmount = (long) takenAmount * (long) stackSize;
        if (!assetHandler.withdrawSkillAmount(player, skillName, totalAmount)) {
            if (chargedFee > 0D) {
                assetHandler.depositSkillAmount(player, skillName, (long) Math.floor(chargedFee));
            }
            requiredItem.rollback(player);
            sendSkillMessage(player, "TransactionFailed",
                    "%prefix% &cThe mcMMO skill withdrawal could not be completed right now.", skillName);
            return false;
        }

        String message = assetHandler.getSkillMessage("Withdraw",
                "%prefix% &eCreated &b%stack%&7%type% skill note(s)&8: &a%amount% &e(&7Total: &a%stacked-amount%&e) &8| &7Skill Balance: &b%balance%");
        message = message.replace("%amount%", assetHandler.formatWithPreSuffix(takenAmount));
        message = message.replace("%stacked-amount%", assetHandler.formatWithPreSuffix(totalAmount));
        message = message.replace("%balance%", assetHandler.formatWithPreSuffix(assetHandler.getSkillBalance(player, skillName)));
        message = Utils.formatStackSize(message, stackSize);
        message = assetHandler.applySkillPlaceholders(message, skillName, player);
        plugin.getUtils().sendMessage(player, message);

        double tax = calculateSkillTax(player);
        ItemStack item = assetHandler.getSkillHandler().getSkillItem(player.getName(), skillName, takenAmount, stackSize, true, tax, null);
        if (player.getInventory().firstEmpty() != -1) {
            Utils.addItem(player, item);
        } else {
            player.getWorld().dropItem(player.getLocation(), item);
        }

        plugin.getWithdrawLogger().logWithdraw(skillHandler(), player, takenAmount, stackSize, totalAmount,
                assetHandler.getSkillBalance(player, skillName));
        playSkillWithdrawSound(player, takenAmount);
        return true;
    }

    private double calculateSkillTax(Player player) {
        if (skillHandler().hasBypassTaxPermission(player)) return 0D;
        double tax = skillHandler().getConfig().getDouble("Settings.Tax.Percentage");
        if (skillHandler().getConfig().getBoolean("Settings.PermissionNotes.Enabled")) {
            for (String key : skillHandler().getConfig().getSection("Settings.PermissionNotes").getRoutesAsStrings(false)) {
                if (skillHandler().hasPermissionNote(player, key)) {
                    tax = skillHandler().getConfig().getDouble("Settings.PermissionNotes." + key + ".Tax.Percentage");
                }
            }
        }
        return tax;
    }

    private void playSkillWithdrawSound(Player player, double amount) {
        me.mraxetv.beastwithdraw.managers.NoteItemSettings.SoundSettings soundSettings = skillHandler().getSoundSettings("Withdraw", amount);
        if (soundSettings == null || !soundSettings.isEnabled()) return;

        try {
            Sound sound = Sound.valueOf(soundSettings.getSound().toUpperCase());
            player.playSound(player.getLocation(), sound, soundSettings.getVolume(), soundSettings.getPitch());
        } catch (Exception exception) {
            Bukkit.getConsoleSender().sendMessage(
                    plugin.getUtils().getPrefix() + "\u00A7cBroken sound in " + skillHandler().getID() + " Withdraw section!");
        }
    }

    private void sendSkillMessage(Player player, String key, String fallback, String skillName) {
        String message = assetHandler.getSkillMessage(key, fallback);
        message = assetHandler.applySkillPlaceholders(message, skillName, player);
        plugin.getUtils().sendMessage(player, message);
    }

    private BeastMcMMORedeemHandler skillHandler() {
        return assetHandler.getSkillHandler() == null ? assetHandler : assetHandler.getSkillHandler();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("all");
            suggestions.add("100");
            suggestions.add("1000");
            suggestions.add("10000");
            if (assetHandler.isSkillNotesEnabled()) {
                suggestions.addAll(assetHandler.getSkillSuggestions());
            }
        } else if (args.length == 2 && assetHandler.isSkillNotesEnabled()
                && assetHandler.normalizeSkillName(args[0]) != null) {
            suggestions.add("all");
            suggestions.add("1");
            suggestions.add("5");
            suggestions.add("10");
            suggestions.add("25");
            suggestions.add("100");
        } else if (args.length == 2) {
            suggestions.add("1");
            suggestions.add("5");
            suggestions.add("10");
            suggestions.add("64");
        } else if (args.length == 3 && assetHandler.isSkillNotesEnabled()
                && assetHandler.normalizeSkillName(args[0]) != null) {
            suggestions.add("1");
            suggestions.add("5");
            suggestions.add("10");
            suggestions.add("64");
        }

        String partial = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ENGLISH);
        return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ENGLISH).startsWith(partial))
                .collect(Collectors.toList());
    }
}
