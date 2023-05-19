package me.mraxetv.beastwithdraw.managers.assets;


import me.mraxetv.beastcore.utils.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.xpbottle.XpBottleCMD;
import me.mraxetv.beastwithdraw.listener.XpBottleRedeemListener;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class XpBottleHandler extends AssetHandler {
    public XpBottleHandler(BeastWithdrawPlugin pl, String id) {
        super(pl, id);
        setMaterial(MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1) ? Material.EXPERIENCE_BOTTLE : Material.valueOf("EXP_BOTTLE"));
        new XpBottleRedeemListener(pl);
        pl.getCommand("XpBottle").setExecutor(pl);
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
