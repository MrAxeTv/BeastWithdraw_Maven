package me.mraxetv.beastwithdraw.managers.assets;


import me.mraxetv.beasttokens.BeastTokensPlugin;
import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beasttokens.api.handlers.BTTokensManager;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.tokenwithdraw.BeastTokenNoteCMD;
import me.mraxetv.beastwithdraw.events.BTokensRedeemEvent;
import me.mraxetv.beastwithdraw.listener.RedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import org.bukkit.entity.Player;

public class BeastTokensHandler extends AssetHandler<Double> {

    private BTTokensManager api;
    private BeastTokenNoteCMD beastTokenNoteCMD;

    public BeastTokensHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        api = BeastTokensAPI.getTokensManager();
        beastTokenNoteCMD = new BeastTokenNoteCMD(pl,this);
        new RedeemListener(pl);
        //Disable BeastTones OG withdraw system
        if(pl.getServer().getPluginManager().getPlugin("BeastTokens") != null) BeastTokensPlugin.getInstance().getDeposit().disable();
        //Register it to RedeemListeners
        RedeemRegistry.register(id, BTokensRedeemEvent::new);
    }

    @Override
    public Double getBalance(Player p) {
        return api.getTokens(p);
    }

    @Override
    public void withdrawAmount(Player p, Double amount) {
        api.removeTokens(p,amount);
    }

    @Override
    public void depositAmount(Player p, Double amount) {

        api.addTokens(p,amount);
    }

    @Override
    public boolean isToBigAmount(double amount) {
        return false;
    }
}
