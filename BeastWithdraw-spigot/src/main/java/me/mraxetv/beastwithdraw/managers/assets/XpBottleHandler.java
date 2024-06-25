package me.mraxetv.beastwithdraw.managers.assets;


import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.xpbottle.XpBottleCMD;
import me.mraxetv.beastwithdraw.listener.XpBottleRedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class XpBottleHandler extends AssetHandler {
    public XpBottleHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        new BukkitRunnable() {
            @Override
            public void run() {
                new XpBottleRedeemListener(pl);
            }
        }.runTaskLater(pl,1);

        pl.getCommand("XpBottle").setExecutor(new XpBottleCMD(pl,this));
    }

    @Override
    public double getBalance(Player p) {

        return XpManager.getTotalExperience(p);
    }

    @Override
    public void withdrawAmount(Player p, double amount) {
        XpManager.setTotalExperience(p, (int) (getBalance(p) - amount));
    }

    @Override
    public void depositAmount(Player p, double amount) {
        XpManager.setTotalExperience(p, (int) (getBalance(p) + amount));

    }


}
