package me.mraxetv.beastwithdraw.managers.assets;

import lombok.Getter;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.commands.cashwithdraw.CashNoteCMD;
import me.mraxetv.beastwithdraw.events.CashRedeemEvent;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CashNoteHandler extends AssetHandler<BigDecimal> {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String EXACT_AMOUNT_KEY_SUFFIX = "_Exact";

    private BeastWithdrawPlugin pl;
    @Getter
    private static Economy econ = null;
    @Getter
    private CashNoteCMD cashNoteCMD;

    public CashNoteHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        this.pl = pl;
        setupEconomy();
        cashNoteCMD = new CashNoteCMD(pl,this);
        RedeemRegistry.register(id, CashRedeemEvent::new);
    }

    @Override
    public BigDecimal getBalance(Player p) {
        return getBalanceDecimal(p);
    }

    @Override
    protected void withdrawAmountExact(Player p, BigDecimal amount) {
        econ.withdrawPlayer(p, amount.doubleValue());
    }

    @Override
    protected void depositAmountExact(Player p, BigDecimal amount) {
        econ.depositPlayer(p, amount.doubleValue());
    }

    @Override
    public boolean isToBigAmount(double amount) {
        return false;
    }

    @Override
    public WithdrawCMD getWithdrawCMD() {
        return cashNoteCMD;
    }

    public BigDecimal getBalanceDecimal(Player player) {
        return normalize(BigDecimal.valueOf(econ.getBalance(player)), RoundingMode.HALF_UP);
    }

    public BigDecimal parseUserAmount(String rawAmount) {
        return normalize(new BigDecimal(rawAmount), RoundingMode.DOWN);
    }

    @Override
    protected BigDecimal convertAmount(double amount) {
        return normalize(BigDecimal.valueOf(amount), RoundingMode.DOWN);
    }

    public BigDecimal normalize(BigDecimal amount, RoundingMode roundingMode) {
        if (amount == null) {
            return ZERO.setScale(getMoneyScale(), RoundingMode.DOWN);
        }
        return amount.setScale(getMoneyScale(), roundingMode);
    }

    public BigDecimal getConfiguredAmount(String path) {
        return normalize(BigDecimal.valueOf(getConfig().getDouble(path)), RoundingMode.DOWN);
    }

    public BigDecimal getStoredAmount(ItemStack itemStack) {
        NBTItem nbtItem = new NBTItem(itemStack);
        String exactAmount = nbtItem.getString(getExactAmountKey());
        if (exactAmount != null && !exactAmount.trim().isEmpty()) {
            try {
                return normalize(new BigDecimal(exactAmount), RoundingMode.DOWN);
            } catch (NumberFormatException ignored) {
            }
        }

        return normalize(BigDecimal.valueOf(nbtItem.getDouble(getNbtTag())), RoundingMode.DOWN);
    }

    public BigDecimal calculateRedeemTax(BigDecimal amount, ItemStack itemStack) {
        NBTItem nbtItem = new NBTItem(itemStack);
        BigDecimal taxPercentage = BigDecimal.valueOf(nbtItem.getDouble("Tax"));
        if (taxPercentage.compareTo(ZERO) <= 0) {
            return ZERO.setScale(getMoneyScale(), RoundingMode.DOWN);
        }

        BigDecimal taxAmount = amount.multiply(taxPercentage)
                .divide(BigDecimal.valueOf(100), getMoneyScale() + 4, RoundingMode.CEILING);
        taxAmount = normalize(taxAmount, RoundingMode.CEILING);

        if (taxAmount.compareTo(amount) > 0) {
            return amount;
        }
        return taxAmount;
    }

    public CashTransactionResult withdrawForNote(Player player, BigDecimal noteAmount, BigDecimal feeAmount) {
        return applyTransaction(player, noteAmount.add(feeAmount), true);
    }

    public CashTransactionResult redeemNote(Player player, BigDecimal amount) {
        return applyTransaction(player, amount, false);
    }

    @Override
    public ItemStack getItem(String owner, double value, int amount, boolean signed, double tax) {
        return getItem(owner, value, amount, signed, tax, null);
    }

    @Override
    public ItemStack getItem(String owner, double value, int amount, boolean signed, double tax, String amountOverrideId) {
        ItemStack itemStack = super.getItem(owner, value, amount, signed, tax, amountOverrideId);
        NBTItem nbtItem = new NBTItem(itemStack);
        nbtItem.setString(getExactAmountKey(), normalize(BigDecimal.valueOf(value), RoundingMode.DOWN).toPlainString());
        return nbtItem.getItem();
    }

    private void setupEconomy() {
        if (!pl.getServer().getPluginManager().isPluginEnabled("Vault")) return;
        RegisteredServiceProvider<Economy> rsp = pl.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        econ = rsp.getProvider();
    }

    private CashTransactionResult applyTransaction(Player player, BigDecimal requestedAmount, boolean withdraw) {
        BigDecimal normalizedAmount = normalize(requestedAmount, RoundingMode.DOWN);
        BigDecimal balanceBefore = getBalanceDecimal(player);

        EconomyResponse response = withdraw
                ? econ.withdrawPlayer(player, normalizedAmount.doubleValue())
                : econ.depositPlayer(player, normalizedAmount.doubleValue());

        if (response == null || !response.transactionSuccess()) {
            return new CashTransactionResult(false, normalizedAmount, balanceBefore, balanceBefore, ZERO.setScale(getMoneyScale(), RoundingMode.DOWN));
        }

        BigDecimal balanceAfter = getBalanceDecimal(player);
        BigDecimal actualDelta = withdraw
                ? balanceBefore.subtract(balanceAfter)
                : balanceAfter.subtract(balanceBefore);
        actualDelta = normalize(actualDelta.max(ZERO), RoundingMode.DOWN);

        if (actualDelta.compareTo(normalizedAmount) < 0) {
            rollbackPartialTransaction(player, actualDelta, withdraw);
            return new CashTransactionResult(false, normalizedAmount, balanceBefore, getBalanceDecimal(player), actualDelta);
        }

        return new CashTransactionResult(true, normalizedAmount, balanceBefore, balanceAfter, actualDelta);
    }

    private void rollbackPartialTransaction(Player player, BigDecimal actualDelta, boolean withdraw) {
        if (actualDelta.compareTo(ZERO) <= 0) {
            return;
        }

        EconomyResponse rollback = withdraw
                ? econ.depositPlayer(player, actualDelta.doubleValue())
                : econ.withdrawPlayer(player, actualDelta.doubleValue());

        if (rollback == null || !rollback.transactionSuccess()) {
            pl.getLogger().warning("Failed to roll back a partial cash transaction for " + player.getName() + " amount=" + actualDelta.toPlainString());
        }
    }

    private int getMoneyScale() {
        if (getConfig().getBoolean("Settings.DisableDecimals")) {
            return 0;
        }

        int providerScale = econ != null ? econ.fractionalDigits() : -1;
        if (providerScale >= 0) {
            return Math.max(0, Math.min(2, providerScale));
        }

        return 2;
    }

    private String getExactAmountKey() {
        return getNbtTag() + EXACT_AMOUNT_KEY_SUFFIX;
    }

    public static final class CashTransactionResult {
        private final boolean success;
        private final BigDecimal requestedAmount;
        private final BigDecimal balanceBefore;
        private final BigDecimal balanceAfter;
        private final BigDecimal actualDelta;

        private CashTransactionResult(boolean success, BigDecimal requestedAmount, BigDecimal balanceBefore, BigDecimal balanceAfter, BigDecimal actualDelta) {
            this.success = success;
            this.requestedAmount = requestedAmount;
            this.balanceBefore = balanceBefore;
            this.balanceAfter = balanceAfter;
            this.actualDelta = actualDelta;
        }

        public boolean isSuccess() {
            return success;
        }

        public BigDecimal getRequestedAmount() {
            return requestedAmount;
        }

        public BigDecimal getBalanceBefore() {
            return balanceBefore;
        }

        public BigDecimal getBalanceAfter() {
            return balanceAfter;
        }

        public BigDecimal getActualDelta() {
            return actualDelta;
        }
    }

}
