package me.mraxetv.beastwithdraw.managers.assets;

import lombok.Getter;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
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
    @Getter
    private  PlayerPointsNoteCMD playerPointsNoteCMD;

    public PlayerPointsHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        playerPointsAPI = PlayerPoints.getInstance().getAPI();
        playerPointsNoteCMD = new PlayerPointsNoteCMD(pl, this);
        RedeemRegistry.register(id, CustomRedeemEvent::new);
        //new PlayerPointsNoteRedeemListener(pl,id);
    }


    @Override
    public Integer getBalance(Player p) {
        return playerPointsAPI.look(p.getUniqueId());
    }

    @Override
    protected void withdrawAmountExact(Player p, Integer amount) {

        playerPointsAPI.take(p.getUniqueId(), amount);

    }

    @Override
    protected void depositAmountExact(Player p, Integer amount) {

        playerPointsAPI.give(p.getUniqueId(),amount);

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
        return playerPointsNoteCMD;
    }
}
