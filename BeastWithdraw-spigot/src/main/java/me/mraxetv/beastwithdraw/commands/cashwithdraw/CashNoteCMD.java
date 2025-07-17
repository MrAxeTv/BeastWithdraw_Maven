package me.mraxetv.beastwithdraw.commands.cashwithdraw;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.assets.CashNoteHandler;
import me.mraxetv.beastwithdraw.managers.assets.PlayerPointsHandler;


public class CashNoteCMD extends WithdrawCMD {
    private final BeastWithdrawPlugin plugin;
    private final CashNoteHandler assetHandler;

    public CashNoteCMD(BeastWithdrawPlugin pl, CashNoteHandler assetHandler) {
        super(pl, assetHandler);
        this.plugin = pl;
        this.assetHandler = assetHandler;

    }
}