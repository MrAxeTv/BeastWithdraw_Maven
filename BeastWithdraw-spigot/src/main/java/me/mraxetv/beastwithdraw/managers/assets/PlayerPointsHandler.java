package me.mraxetv.beastwithdraw.managers.assets;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.playerpoints.PlayerPointsNoteCMD;
import me.mraxetv.beastwithdraw.listener.PlayerPointsNoteRedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;

public class PlayerPointsHandler extends AssetHandler {


    private PlayerPointsAPI playerPointsAPI;

    public PlayerPointsHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        playerPointsAPI = PlayerPoints.getInstance().getAPI();
        pl.getCommand("bpWithdraw").setExecutor(new PlayerPointsNoteCMD(pl,id));
        new PlayerPointsNoteRedeemListener(pl,id);
    }


    @Override
    public double getBalance(Player p) {
        return playerPointsAPI.look(p.getUniqueId());
    }

    @Override
    public void withdrawAmount(Player p, double amount) {

        playerPointsAPI.take(p.getUniqueId(), (int)amount);

    }

    @Override
    public void depositAmount(Player p, double amount) {

        playerPointsAPI.give(p.getUniqueId(),(int)amount);

    }
}
