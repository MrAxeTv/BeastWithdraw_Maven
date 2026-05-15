package me.mraxetv.beastwithdraw.managers.assets;

import lombok.Getter;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.commands.xpbottle.XpBottleCMD;
import me.mraxetv.beastwithdraw.events.BottleRedeemEvent;
import me.mraxetv.beastwithdraw.listener.XpBottleRedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.entity.Player;

public class XpBottleHandler extends AssetHandler<Integer> {
    @Getter
    private WithdrawCMD xpBottleCMD;

    public XpBottleHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        new XpBottleRedeemListener(pl,this);
        xpBottleCMD = new XpBottleCMD(pl, this);
        RedeemRegistry.register(id, BottleRedeemEvent::new);
    }

    @Override
    public Integer getBalance(Player p) {
        return XpManager.getTotalExperience(p);
    }

    @Override
    protected void withdrawAmountExact(Player p, Integer amount) {
        XpManager.setTotalExperience(p, getBalance(p) - amount);
    }

    @Override
    protected void depositAmountExact(Player p, Integer amount) {
        XpManager.setTotalExperience(p, getBalance(p) + amount);

    }

    @Override
    protected Integer convertAmount(double amount) {
        return Double.valueOf(amount).intValue();
    }

    @Override
    public boolean isToBigAmount(double amount) {
        return amount > Integer.MAX_VALUE;
    }

    public WithdrawCMD getWithdrawCMD() {
        return xpBottleCMD;
    }


}
