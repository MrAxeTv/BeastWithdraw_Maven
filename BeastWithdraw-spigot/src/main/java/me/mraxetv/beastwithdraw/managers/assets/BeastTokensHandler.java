package me.mraxetv.beastwithdraw.managers.assets;


import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beasttokens.api.handlers.BTTokensManager;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.tokenwithdraw.BeastTokenNoteCMD;
import me.mraxetv.beastwithdraw.listener.BTokensNoteRedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import org.bukkit.entity.Player;

public class BeastTokensHandler extends AssetHandler {

    private BTTokensManager api;

    public BeastTokensHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        api = BeastTokensAPI.getTokensManager();
        pl.getCommand("btWithdraw").setExecutor(new BeastTokenNoteCMD(pl,this));
        new BTokensNoteRedeemListener(pl);
    }

    @Override
    public double getBalance(Player p) {
        return api.getTokens(p);
    }

    @Override
    public void withdrawAmount(Player p, double amount) {
        api.removeTokens(p,amount);
    }

    @Override
    public void depositAmount(Player p, double amount) {

        api.addTokens(p,amount);
    }
}
