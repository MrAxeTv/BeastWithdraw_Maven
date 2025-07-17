package me.mraxetv.beastwithdraw.managers.assets;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.playerpoints.PlayerPointsNoteCMD;
import me.mraxetv.beastwithdraw.events.BTokensRedeemEvent;
import me.mraxetv.beastwithdraw.events.CustomRedeemEvent;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;

public class PlayerPointsHandler extends AssetHandler<Integer> {

    private PlayerPointsAPI playerPointsAPI;
    private  PlayerPointsNoteCMD playerPointsNoteCMD;

    public PlayerPointsHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        playerPointsAPI = PlayerPoints.getInstance().getAPI();
        playerPointsNoteCMD = new PlayerPointsNoteCMD(pl, this);
        RedeemRegistry.register(id, CustomRedeemEvent::new);
        //new PlayerPointsNoteRedeemListener(pl,id);
    }


    @Override
    public Double getBalance(Player p) {
        return (double) playerPointsAPI.look(p.getUniqueId());
    }

    @Override
    public void withdrawAmount(Player p, Double amount) {

        playerPointsAPI.take(p.getUniqueId(), amount.intValue());

    }

    @Override
    public void depositAmount(Player p, Double amount) {

        playerPointsAPI.give(p.getUniqueId(),amount.intValue());

    }

    @Override
    public boolean isToBigAmount(double amount) {
        return amount > Integer.MAX_VALUE;
    }
}
