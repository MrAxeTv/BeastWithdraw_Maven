package me.mraxetv.beastwithdraw.commands.playerpoints;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.managers.assets.PlayerPointsHandler;


public class PlayerPointsNoteCMD extends WithdrawCMD {

    private final BeastWithdrawPlugin plugin;
    private final PlayerPointsHandler assetHandler;

    public PlayerPointsNoteCMD(BeastWithdrawPlugin plugin, PlayerPointsHandler assetHandler) {
        super(plugin, assetHandler);
        this.plugin = plugin;
        this.assetHandler = assetHandler;
    }





}
