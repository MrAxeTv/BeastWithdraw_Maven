package me.mraxetv.beastwithdraw.managers;

import lombok.Getter;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.listener.RedeemListener;
import me.mraxetv.beastwithdraw.managers.assets.BeastTokensHandler;
import me.mraxetv.beastwithdraw.managers.assets.BeastLifeStealHandler;
import me.mraxetv.beastwithdraw.managers.assets.CashNoteHandler;
import me.mraxetv.beastwithdraw.managers.assets.PlayerPointsHandler;
import me.mraxetv.beastwithdraw.managers.assets.XpBottleHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemEventFactory;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WithdrawManager {

    private BeastWithdrawPlugin pl;
    public static XpBottleHandler XP_BOTTLE = null;
    public static CashNoteHandler CASH_NOTE = null;
    public static BeastTokensHandler BEASTTOKENS_NOTE = null;
    public static PlayerPointsHandler PLAYERPOINTS_NOTE = null;
    public static BeastLifeStealHandler BEASTLIFESTEAL_NOTE = null;



    private static HashMap<String, AssetHandler> assetsList;
    private static HashMap<String, String> assetAliases;

    public WithdrawManager(BeastWithdrawPlugin pl) {
        this.pl = pl;
        initAssets();
    }

    public void initAssets() {
        assetsList = new HashMap<>();
        assetAliases = new HashMap<>();
        XP_BOTTLE = null;
        CASH_NOTE = null;
        BEASTTOKENS_NOTE = null;
        PLAYERPOINTS_NOTE = null;
        BEASTLIFESTEAL_NOTE = null;

        //Registering Listener
        new RedeemListener(pl);
        if (pl.getSettings().getBoolean("Settings.Withdraws.XpBottle.Enabled")) {
            assetsList.put("xpbottle", XP_BOTTLE = new XpBottleHandler(BeastWithdrawPlugin.getInstance(), "XpBottle"));
            pl.getWithdrawLogger().prepareAssetLogs(XP_BOTTLE);
        }

        if (pl.getSettings().getBoolean("Settings.Withdraws.CashNote.Enabled")) {
            if ((pl.getServer().getPluginManager().isPluginEnabled("Vault"))) {
                assetsList.put("cashnote", CASH_NOTE = new CashNoteHandler(BeastWithdrawPlugin.getInstance(), "CashNote"));
                pl.getWithdrawLogger().prepareAssetLogs(CASH_NOTE);
            } else {
                pl.getServer().getConsoleSender().sendMessage("[" + pl.getDescription().getPrefix() + "] Server is missing 'Vault' plugin which you need for economy(money) to work!");
            }
        }

        if (pl.getSettings().getBoolean("Settings.Withdraws.BeastTokensNote.Enabled")) {
            if ((pl.getServer().getPluginManager().isPluginEnabled("BeastTokens"))) {
                assetsList.put("beasttokensnote", BEASTTOKENS_NOTE = new BeastTokensHandler(BeastWithdrawPlugin.getInstance(), "BeastTokensNote"));
                pl.getWithdrawLogger().prepareAssetLogs(BEASTTOKENS_NOTE);
            } else {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cServer is missing 'BeastTokens' plugin which you need for 'Tokens Note' to work!");
            }
        }
        if (pl.getSettings().getBoolean("Settings.Withdraws.PlayerPointsNote.Enabled")) {
            if ((pl.getServer().getPluginManager().isPluginEnabled("PlayerPoints"))) {
                assetsList.put("playerpointsnote", PLAYERPOINTS_NOTE = new PlayerPointsHandler(BeastWithdrawPlugin.getInstance(), "PlayerPointsNote"));
                pl.getWithdrawLogger().prepareAssetLogs(PLAYERPOINTS_NOTE);
            } else {
                
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cServer is missing 'PlayerPoints' plugin which you need for 'PlayerPoints Note' to work!");
            }
        }
        if (pl.getSettings().getBoolean("Settings.Withdraws.BeastLifeSteal.Enabled")) {
            if (pl.getServer().getPluginManager().isPluginEnabled("BeastLifeSteal")) {
                assetsList.put("heartwithdraw", BEASTLIFESTEAL_NOTE = new BeastLifeStealHandler(BeastWithdrawPlugin.getInstance(), "heartwithdraw"));
                pl.getWithdrawLogger().prepareAssetLogs(BEASTLIFESTEAL_NOTE);
                registerAssetAliases("heartwithdraw",
                        "beastlifesteal",
                        "heartnote",
                        "heartnotes",
                        "beastlifestealnote",
                        "beastlifestealnotes");
            } else {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cServer is missing 'BeastLifeSteal' plugin which you need for 'Heart Withdraw' to work!");
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Bukkit.getServer().getClass().getMethod("syncCommands").invoke(Bukkit.getServer());
                } catch (Exception e) {}
            }
        }.runTaskLater(pl,1);

    }

    public List<String> getAssetHandlerList(){
        return new ArrayList<>(assetsList.keySet());
    }

    public AssetHandler getAssetHandler(String id) {
        String resolvedId = resolveAssetId(id);
        return resolvedId == null ? null : assetsList.get(resolvedId);
    }

    public boolean hasAssetHandler(String id) {
        return resolveAssetId(id) != null;
    }

    public void unregisterAssetHandlers() {

        if (assetsList != null) {
            for (AssetHandler handler : assetsList.values()) {
                if (handler != null && handler.getWithdrawCMD() != null) {
                    handler.getWithdrawCMD().unRegisterBukkitCommand();
                }
            }
            assetsList.clear();
        }

        if (assetAliases != null) {
            assetAliases.clear();
        }

    }

    private void registerAssetAliases(String primaryId, String... aliases) {
        if (aliases == null) {
            return;
        }

        for (String alias : aliases) {
            if (alias == null || alias.trim().isEmpty()) {
                continue;
            }
            assetAliases.put(alias.toLowerCase(), primaryId.toLowerCase());
        }
    }

    private String resolveAssetId(String id) {
        if (id == null) {
            return null;
        }

        String normalizedId = id.trim().toLowerCase();
        if (normalizedId.isEmpty()) {
            return null;
        }

        if (assetsList.containsKey(normalizedId)) {
            return normalizedId;
        }

        return assetAliases.get(normalizedId);
    }

}

