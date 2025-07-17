package me.mraxetv.beastwithdraw.managers.assets;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.cashwithdraw.CashNoteCMD;
import me.mraxetv.beastwithdraw.events.CashRedeemEvent;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class CashNoteHandler extends AssetHandler<Double> {

    private BeastWithdrawPlugin pl;
    private static Economy econ = null;
    private CashNoteCMD cashNoteCMD;

    public CashNoteHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        this.pl = pl;
        setupEconomy();
        cashNoteCMD = new CashNoteCMD(pl,this);
        RedeemRegistry.register(id, CashRedeemEvent::new);
    }

    @Override
    public Double getBalance(Player p) {
        return econ.getBalance(p);
    }

    @Override
    public void withdrawAmount(Player p, Double amount) {

        econ.withdrawPlayer(p,amount);
    }

    @Override
    public void depositAmount(Player p, Double amount) {
        econ.depositPlayer(p,amount);
    }

    @Override
    public boolean isToBigAmount(double amount) {
        return false;
    }

    public static Economy getEcon() {
        return econ;
    }

    private void setupEconomy() {
        if (!pl.getServer().getPluginManager().isPluginEnabled("Vault")) return;
        RegisteredServiceProvider<Economy> rsp = pl.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        econ = rsp.getProvider();
    }

}
