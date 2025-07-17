package me.mraxetv.beastwithdraw.managers.assets;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.xpbottle.XpBottleCMD;
import me.mraxetv.beastwithdraw.events.BottleRedeemEvent;
import me.mraxetv.beastwithdraw.listener.XpBottleRedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.entity.Player;

public class XpBottleHandler extends AssetHandler<Integer> {
    private XpBottleCMD xpBottleCMD;

    public XpBottleHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        new XpBottleRedeemListener(pl,this);
        xpBottleCMD = new XpBottleCMD(pl, this);
        RedeemRegistry.register(id, BottleRedeemEvent::new);
    }

    @Override
    public Double getBalance(Player p) {

        return (double)XpManager.getTotalExperience(p);
    }

    @Override
    public void withdrawAmount(Player p, Double amount) {
        XpManager.setTotalExperience(p, (getBalance(p).intValue() - amount.intValue()));
    }

    @Override
    public void depositAmount(Player p, Double amount) {
        XpManager.setTotalExperience(p, (getBalance(p).intValue() + amount.intValue()));

    }

    @Override
    public boolean isToBigAmount(double amount) {
        return amount > Integer.MAX_VALUE;
    }


}
