package me.mraxetv.beastwithdraw.managers;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.AliasesRegistration;
import me.mraxetv.beastwithdraw.managers.assets.BeastTokensHandler;
import me.mraxetv.beastwithdraw.managers.assets.CashNoteHandler;
import me.mraxetv.beastwithdraw.managers.assets.PlayerPointsHandler;
import me.mraxetv.beastwithdraw.managers.assets.XpBottleHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WithdrawManager {

    private BeastWithdrawPlugin pl;
    public static AssetHandler XP_BOTTLE = null;
    public static AssetHandler CASH_NOTE = null;
    public static AssetHandler BEASTTOKENS_NOTE = null;
    public static AssetHandler PLAYERPOINTS_NOTE = null;


    private static HashMap<String, AssetHandler> assetsList;

    public WithdrawManager(BeastWithdrawPlugin pl) {
        this.pl = pl;
        initAssets();
    }


    public void initAssets() {
        assetsList = new HashMap<>();

        if (pl.getConfig().getBoolean("Settings.Withdraws.XpBottle.Enabled"))
            assetsList.put("xpbottle", XP_BOTTLE = new XpBottleHandler(BeastWithdrawPlugin.getInstance(), "XpBottle"));

        if (pl.getConfig().getBoolean("Settings.Withdraws.CashNote.Enabled")) {
            if ((pl.getServer().getPluginManager().isPluginEnabled("Vault"))) {
                assetsList.put("cashnote", CASH_NOTE = new CashNoteHandler(BeastWithdrawPlugin.getInstance(), "CashNote"));
            } else {
                pl.getServer().getConsoleSender().sendMessage("[" + pl.getDescription().getPrefix() + "] Server is missing 'Vault' plugin which you need for economy(money) to work!");
            }
        }

        if (pl.getConfig().getBoolean("Settings.Withdraws.BeastTokensNote.Enabled")) {
            if ((pl.getServer().getPluginManager().isPluginEnabled("BeastTokens"))) {
                assetsList.put("beasttokensnote", BEASTTOKENS_NOTE = new BeastTokensHandler(BeastWithdrawPlugin.getInstance(), "BeastTokensNote"));
            } else {
                Utils.sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cServer is missing 'BeastTokens' plugin which you need for 'Tokens Note' to work!");
            }
        }
        if (pl.getConfig().getBoolean("Settings.Withdraws.PlayerPointsNote.Enabled")) {
            if ((pl.getServer().getPluginManager().isPluginEnabled("PlayerPoints"))) {
                assetsList.put("playerpointsnote", PLAYERPOINTS_NOTE = new PlayerPointsHandler(BeastWithdrawPlugin.getInstance(), "PlayerPointsNote"));
            } else {
                Utils.sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cServer is missing 'PlayerPoints' plugin which you need for 'PlayerPoints Note' to work!");
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                AliasesRegistration.syncCommands();
            }
        }.runTaskLater(pl,2);

    }

    public static List<String> getAssetHandlerList(){
        return new ArrayList<>(assetsList.keySet());
    }

    public static AssetHandler getAssetHandler(String id) {

        return assetsList.get(id.toLowerCase());
    }

    public static boolean hasAssetHandler(String id) {
        return assetsList.containsKey(id.toLowerCase());
    }

}

