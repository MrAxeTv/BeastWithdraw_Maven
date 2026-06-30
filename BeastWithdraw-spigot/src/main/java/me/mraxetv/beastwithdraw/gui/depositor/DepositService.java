package me.mraxetv.beastwithdraw.gui.depositor;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.NoteItemSettings;
import me.mraxetv.beastwithdraw.managers.assets.BeastLifeStealHandler;
import me.mraxetv.beastwithdraw.managers.assets.BeastMcMMORedeemHandler;
import me.mraxetv.beastwithdraw.managers.assets.CashNoteHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DepositService {
    private final BeastWithdrawPlugin plugin;

    DepositService(BeastWithdrawPlugin plugin) {
        this.plugin = plugin;
    }

    DepositResult deposit(Player player, Inventory inventory, List<Integer> depositSlots, DepositGuiProfile profile) {
        DepositResult result = new DepositResult();

        for (Integer slot : depositSlots) {
            if (slot == null || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            ItemStack itemStack = inventory.getItem(slot);
            if (isEmpty(itemStack)) {
                continue;
            }

            NoteData noteData = readNote(itemStack);
            if (noteData == null) {
                result.invalidItems += itemStack.getAmount();
                continue;
            }

            if (!profile.accepts(noteData.assetHandler)) {
                result.unsupportedItems += itemStack.getAmount();
                continue;
            }

            if (!noteData.assetHandler.hasRedeemPermission(player)) {
                result.noPermissionItems += itemStack.getAmount();
                continue;
            }

            if (noteData.assetHandler instanceof CashNoteHandler) {
                processCash(player, inventory, slot, itemStack, (CashNoteHandler) noteData.assetHandler, result);
                continue;
            }

            if (noteData.assetHandler instanceof BeastLifeStealHandler) {
                processHearts(player, inventory, slot, itemStack, (BeastLifeStealHandler) noteData.assetHandler, noteData.amount, result);
                continue;
            }

            if (noteData.assetHandler instanceof BeastMcMMORedeemHandler
                    && ((BeastMcMMORedeemHandler) noteData.assetHandler).isSkillNote(itemStack)) {
                processMcMMOSkill(player, inventory, slot, itemStack, (BeastMcMMORedeemHandler) noteData.assetHandler, noteData.amount, result);
                continue;
            }

            processGeneric(player, inventory, slot, itemStack, noteData.assetHandler, noteData.amount, result);
        }

        return result;
    }

    NoteData readNote(ItemStack itemStack) {
        if (isEmpty(itemStack) || !itemStack.hasItemMeta()) {
            return null;
        }

        NBTItem nbtItem = new NBTItem(itemStack);
        if (!nbtItem.hasKey("RedeemType")) {
            return null;
        }

        String type = nbtItem.getString("RedeemType");
        AssetHandler assetHandler = plugin.getWithdrawManager().getAssetHandler(type);
        if (assetHandler == null || !nbtItem.hasKey(assetHandler.getNbtTag())) {
            return null;
        }

        double amount = assetHandler instanceof CashNoteHandler
                ? ((CashNoteHandler) assetHandler).getStoredAmount(itemStack).doubleValue()
                : nbtItem.getDouble(assetHandler.getNbtTag());
        if (amount <= 0) {
            return null;
        }

        return new NoteData(type, assetHandler, amount);
    }

    boolean isDepositNote(ItemStack itemStack) {
        return readNote(itemStack) != null;
    }

    boolean isDepositNote(ItemStack itemStack, DepositGuiProfile profile) {
        NoteData noteData = readNote(itemStack);
        return noteData != null && profile.accepts(noteData.assetHandler);
    }

    private void processCash(Player player, Inventory inventory, int slot, ItemStack itemStack,
                             CashNoteHandler assetHandler, DepositResult result) {
        int stackSize = itemStack.getAmount();
        BigDecimal singleAmount = assetHandler.getStoredAmount(itemStack);
        BigDecimal singleTax = assetHandler.calculateRedeemTax(singleAmount, itemStack);
        BigDecimal totalAmount = singleAmount.multiply(BigDecimal.valueOf(stackSize));
        BigDecimal totalTax = singleTax.multiply(BigDecimal.valueOf(stackSize));
        BigDecimal finalAmount = totalAmount.subtract(totalTax);

        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            result.failedItems += stackSize;
            return;
        }

        CashNoteHandler.CashTransactionResult transaction = assetHandler.redeemNote(player, finalAmount);
        if (!transaction.isSuccess()) {
            result.failedItems += stackSize;
            return;
        }

        inventory.setItem(slot, null);
        result.addSummary(assetHandler, stackSize, totalAmount.doubleValue(), totalTax.doubleValue(),
                finalAmount.doubleValue(), transaction.getBalanceAfter().doubleValue());

        plugin.getWithdrawLogger().logRedeem(
                assetHandler,
                player,
                singleAmount.doubleValue(),
                stackSize,
                totalAmount.doubleValue(),
                totalTax.doubleValue(),
                finalAmount.doubleValue(),
                transaction.getBalanceAfter().doubleValue()
        );
        playRedeemSound(assetHandler, player, singleAmount.doubleValue(), itemStack);
    }

    private void processGeneric(Player player, Inventory inventory, int slot, ItemStack itemStack,
                                AssetHandler assetHandler, double singleAmount, DepositResult result) {
        int stackSize = itemStack.getAmount();
        double singleTax = Math.ceil(assetHandler.calculateTax(singleAmount, itemStack));
        double singleAfterTax = Math.max(0, singleAmount - singleTax);
        double totalAmount = singleAmount * stackSize;
        double totalTax = singleTax * stackSize;
        double finalAmount = singleAfterTax * stackSize;

        if (assetHandler instanceof BeastMcMMORedeemHandler) {
            long capacity = ((BeastMcMMORedeemHandler) assetHandler).getRedeemableCapacity(player);
            if (capacity < (long) Math.ceil(finalAmount)) {
                result.capacityItems += stackSize;
                return;
            }
        }

        assetHandler.depositAmount(player, finalAmount);
        inventory.setItem(slot, null);
        double balanceAfter = assetHandler.getBalanceAsDouble(player);
        result.addSummary(assetHandler, stackSize, totalAmount, totalTax, finalAmount, balanceAfter);

        plugin.getWithdrawLogger().logRedeem(
                assetHandler,
                player,
                singleAmount,
                stackSize,
                totalAmount,
                totalTax,
                finalAmount,
                balanceAfter
        );
        playRedeemSound(assetHandler, player, singleAmount, itemStack);
    }

    private void processMcMMOSkill(Player player, Inventory inventory, int slot, ItemStack itemStack,
                                   BeastMcMMORedeemHandler assetHandler, double singleAmount, DepositResult result) {
        String skillName = assetHandler.getSkillName(itemStack);
        if (skillName == null) {
            result.failedItems += itemStack.getAmount();
            return;
        }

        int stackSize = itemStack.getAmount();
        double singleTax = Math.ceil(assetHandler.calculateTax(singleAmount, itemStack));
        double singleAfterTax = Math.max(0D, singleAmount - singleTax);
        double totalAmount = singleAmount * stackSize;
        double totalTax = singleTax * stackSize;
        double finalAmount = singleAfterTax * stackSize;
        if (finalAmount <= 0D) {
            result.failedItems += stackSize;
            return;
        }

        if (!assetHandler.depositSkillAmount(player, skillName, (long) Math.floor(finalAmount))) {
            result.failedItems += stackSize;
            return;
        }

        inventory.setItem(slot, null);
        double balanceAfter = assetHandler.getSkillBalance(player, skillName);
        result.addSummary(assetHandler, stackSize, totalAmount, totalTax, finalAmount, balanceAfter);

        plugin.getWithdrawLogger().logRedeem(
                assetHandler,
                player,
                singleAmount,
                stackSize,
                totalAmount,
                totalTax,
                finalAmount,
                balanceAfter
        );
        playRedeemSound(assetHandler, player, singleAmount, itemStack);
    }

    private void processHearts(Player player, Inventory inventory, int slot, ItemStack itemStack,
                               BeastLifeStealHandler assetHandler, double rawAmount, DepositResult result) {
        int stackSize = itemStack.getAmount();
        int redeemableCapacity = assetHandler.getRedeemableCapacity(player);
        if (redeemableCapacity <= 0) {
            result.capacityItems += stackSize;
            return;
        }

        int singleAmount = (int) Math.floor(rawAmount);
        int singleTax = (int) Math.ceil(assetHandler.calculateTax(singleAmount, itemStack));
        int singleAfterTax = Math.max(0, singleAmount - singleTax);
        if (singleAfterTax <= 0) {
            result.failedItems += stackSize;
            return;
        }

        HeartDeposit heartDeposit = calculateHeartDeposit(stackSize, redeemableCapacity, singleAmount, singleTax, singleAfterTax);
        if (heartDeposit.redeemedAmount <= 0) {
            result.capacityItems += stackSize;
            return;
        }

        assetHandler.depositAmount(player, (double) heartDeposit.redeemedAmount);
        applyHeartRemainder(player, inventory, slot, itemStack, assetHandler, heartDeposit, singleAmount);

        double balanceAfter = assetHandler.getBalanceAsDouble(player);
        result.addSummary(assetHandler, heartDeposit.consumedItems,
                heartDeposit.redeemedAmount + heartDeposit.totalTax,
                heartDeposit.totalTax,
                heartDeposit.redeemedAmount,
                balanceAfter);

        plugin.getWithdrawLogger().logRedeem(
                assetHandler,
                player,
                singleAmount,
                heartDeposit.consumedItems,
                heartDeposit.redeemedAmount + heartDeposit.totalTax,
                heartDeposit.totalTax,
                heartDeposit.redeemedAmount,
                balanceAfter
        );
        playRedeemSound(assetHandler, player, singleAmount, itemStack);
    }

    private HeartDeposit calculateHeartDeposit(int stackSize, int redeemableCapacity,
                                               int singleAmount, int singleTax, int singleAfterTax) {
        if (singleTax > 0) {
            int fullItemsToRedeem = Math.min(stackSize, redeemableCapacity / singleAfterTax);
            if (fullItemsToRedeem <= 0) {
                return HeartDeposit.none();
            }
            return new HeartDeposit(
                    fullItemsToRedeem * singleAfterTax,
                    fullItemsToRedeem * singleTax,
                    fullItemsToRedeem,
                    fullItemsToRedeem,
                    0,
                    stackSize - fullItemsToRedeem
            );
        }

        int totalRedeemable = Math.min(redeemableCapacity, singleAfterTax * stackSize);
        if (totalRedeemable <= 0) {
            return HeartDeposit.none();
        }

        int fullItemsToRedeem = totalRedeemable / singleAfterTax;
        int partialRedeem = totalRedeemable % singleAfterTax;
        if (partialRedeem == 0) {
            return new HeartDeposit(totalRedeemable, 0, fullItemsToRedeem, fullItemsToRedeem, 0, stackSize - fullItemsToRedeem);
        }

        int consumedItems = fullItemsToRedeem + 1;
        return new HeartDeposit(totalRedeemable, 0, consumedItems, consumedItems, partialRedeem, stackSize - consumedItems);
    }

    private void applyHeartRemainder(Player player, Inventory inventory, int slot, ItemStack original,
                                     BeastLifeStealHandler assetHandler, HeartDeposit heartDeposit, int singleAmount) {
        if (heartDeposit.partialRedeem > 0) {
            double remainingAmount = Math.max(0, singleAmount - heartDeposit.partialRedeem);
            ItemStack partialItem = createPartialHeartItem(original, assetHandler, remainingAmount);
            inventory.setItem(slot, partialItem);

            if (heartDeposit.remainingFullItems > 0) {
                ItemStack remainingStack = original.clone();
                remainingStack.setAmount(heartDeposit.remainingFullItems);
                giveOrDrop(player, remainingStack);
            }
            return;
        }

        int remaining = original.getAmount() - heartDeposit.consumedItems;
        if (remaining > 0) {
            original.setAmount(remaining);
            inventory.setItem(slot, original);
        } else {
            inventory.setItem(slot, null);
        }
    }

    private ItemStack createPartialHeartItem(ItemStack original, BeastLifeStealHandler assetHandler, double amount) {
        NBTItem nbtItem = new NBTItem(original);
        String signer = nbtItem.getString("Signer");
        boolean signed = nbtItem.getBoolean("Signed");
        double tax = nbtItem.getDouble("Tax");
        return assetHandler.getItem(signer == null ? "" : signer, amount, 1, signed, tax, assetHandler.getAmountOverrideId(original));
    }

    private void giveOrDrop(Player player, ItemStack itemStack) {
        if (isEmpty(itemStack)) {
            return;
        }

        player.getInventory().addItem(itemStack).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void playRedeemSound(AssetHandler assetHandler, Player player, double amount, ItemStack itemStack) {
        String amountOverrideId = assetHandler.getAmountOverrideId(itemStack);
        NoteItemSettings.SoundSettings soundSettings = assetHandler.getSoundSettings("Redeem", amount, amountOverrideId);
        if (soundSettings == null || !soundSettings.isEnabled()) {
            return;
        }

        assetHandler.playConfiguredSound(player, "Redeem", amount, "Depositor redeem", amountOverrideId);
    }

    private boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0;
    }

    static final class NoteData {
        private final String type;
        private final AssetHandler assetHandler;
        private final double amount;

        private NoteData(String type, AssetHandler assetHandler, double amount) {
            this.type = type;
            this.assetHandler = assetHandler;
            this.amount = amount;
        }

        String getType() {
            return type;
        }

        AssetHandler getAssetHandler() {
            return assetHandler;
        }

        double getAmount() {
            return amount;
        }
    }

    static final class DepositResult {
        private final Map<String, DepositSummary> summaries = new LinkedHashMap<>();
        private int invalidItems;
        private int unsupportedItems;
        private int noPermissionItems;
        private int failedItems;
        private int capacityItems;

        void addSummary(AssetHandler assetHandler, int notes, double totalAmount, double totalTax, double finalAmount, double balanceAfter) {
            String key = assetHandler.getConfigName().toLowerCase();
            DepositSummary summary = summaries.get(key);
            if (summary == null) {
                summary = new DepositSummary(assetHandler);
                summaries.put(key, summary);
            }

            summary.notes += notes;
            summary.totalAmount += totalAmount;
            summary.totalTax += totalTax;
            summary.finalAmount += finalAmount;
            summary.balanceAfter = balanceAfter;
        }

        boolean hasDeposited() {
            return !summaries.isEmpty();
        }

        Collection<DepositSummary> getSummaries() {
            return summaries.values();
        }

        int getInvalidItems() {
            return invalidItems;
        }

        int getUnsupportedItems() {
            return unsupportedItems;
        }

        int getNoPermissionItems() {
            return noPermissionItems;
        }

        int getFailedItems() {
            return failedItems;
        }

        int getCapacityItems() {
            return capacityItems;
        }
    }

    static final class DepositSummary {
        private final AssetHandler assetHandler;
        private int notes;
        private double totalAmount;
        private double totalTax;
        private double finalAmount;
        private double balanceAfter;

        private DepositSummary(AssetHandler assetHandler) {
            this.assetHandler = assetHandler;
        }

        AssetHandler getAssetHandler() {
            return assetHandler;
        }

        int getNotes() {
            return notes;
        }

        double getTotalAmount() {
            return totalAmount;
        }

        double getTotalTax() {
            return totalTax;
        }

        double getFinalAmount() {
            return finalAmount;
        }

        double getBalanceAfter() {
            return balanceAfter;
        }
    }

    private static final class HeartDeposit {
        private final int redeemedAmount;
        private final int totalTax;
        private final int consumedItems;
        private final int loggedItems;
        private final int partialRedeem;
        private final int remainingFullItems;

        private HeartDeposit(int redeemedAmount, int totalTax, int consumedItems,
                             int loggedItems, int partialRedeem, int remainingFullItems) {
            this.redeemedAmount = redeemedAmount;
            this.totalTax = totalTax;
            this.consumedItems = consumedItems;
            this.loggedItems = loggedItems;
            this.partialRedeem = partialRedeem;
            this.remainingFullItems = remainingFullItems;
        }

        private static HeartDeposit none() {
            return new HeartDeposit(0, 0, 0, 0, 0, 0);
        }
    }
}
