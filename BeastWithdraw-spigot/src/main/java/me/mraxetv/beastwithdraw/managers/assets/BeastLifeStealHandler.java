package me.mraxetv.beastwithdraw.managers.assets;

import lombok.Getter;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.commands.beastlifesteal.HeartWithdrawCMD;
import me.mraxetv.beastwithdraw.events.HeartRedeemEvent;
import me.mraxetv.beastwithdraw.heartwithdraw.BeastLifeStealHook;
import me.mraxetv.beastwithdraw.listener.HeartRedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import org.bukkit.entity.Player;

public class BeastLifeStealHandler extends AssetHandler<Integer> {

    private final BeastLifeStealHook hook;
    @Getter
    private final HeartWithdrawCMD heartWithdrawCMD;

    public BeastLifeStealHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id, "BeastLifeSteal.yml");
        removeLegacyWithdrawPermissionConfig();
        this.hook = new BeastLifeStealHook();
        new HeartRedeemListener(pl, this);
        this.heartWithdrawCMD = new HeartWithdrawCMD(pl, this);
        RedeemRegistry.register(id, HeartRedeemEvent::new);
    }

    public boolean isAvailable() {
        return hook.isAvailable();
    }

    public boolean isGracePeriodActive() {
        return hook.isGracePeriodActive();
    }

    public boolean wouldWithdrawEliminate(Player player, int amount) {
        return hook.wouldWithdrawEliminate(player, amount);
    }

    public int getRedeemableCapacity(Player player) {
        return hook.getRedeemableCapacity(player);
    }

    @Override
    public Integer getBalance(Player p) {
        return hook.getStoredHearts(p);
    }

    @Override
    protected void withdrawAmountExact(Player p, Integer amount) {
        hook.removeHeartsForWithdraw(p, amount);
    }

    @Override
    protected void depositAmountExact(Player p, Integer amount) {
        hook.addHeartsFromRedeem(p, amount);
    }

    @Override
    protected Integer convertAmount(double amount) {
        return Double.valueOf(amount).intValue();
    }

    @Override
    public boolean isToBigAmount(double amount) {
        return amount > Integer.MAX_VALUE;
    }

    @Override
    public WithdrawCMD getWithdrawCMD() {
        return heartWithdrawCMD;
    }

    private void removeLegacyWithdrawPermissionConfig() {
        if (!getConfig().contains("Settings.Permission")) {
            return;
        }
        getConfig().remove("Settings.Permission");
        saveConfig();
    }
}
