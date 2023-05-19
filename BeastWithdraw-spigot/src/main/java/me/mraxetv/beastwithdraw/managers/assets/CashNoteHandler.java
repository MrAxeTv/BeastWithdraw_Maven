package me.mraxetv.beastwithdraw.managers.assets;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.cashwithdraw.CashNoteCMD;
import me.mraxetv.beastwithdraw.listener.CashNoteRedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class CashNoteHandler extends AssetHandler {

    private BeastWithdrawPlugin pl;
    private static Economy econ = null;

    public CashNoteHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        this.pl = pl;
        setupEconomy();
        new CashNoteRedeemListener(pl);
        pl.getCommand("bWithdraw").setExecutor(new CashNoteCMD(pl,this));
    }

    @Override
    public double getBalance(Player p) {
        return econ.getBalance(p);
    }

    @Override
    public void withdrawAmount(Player p, double amount) {

        econ.withdrawPlayer(p,amount);
    }

    @Override
    public void depositAmount(Player p, double amount) {
        econ.depositPlayer(p,amount);
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
