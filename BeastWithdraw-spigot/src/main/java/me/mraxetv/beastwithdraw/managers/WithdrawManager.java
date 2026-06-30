package me.mraxetv.beastwithdraw.managers;

import lombok.Getter;
import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beasttokens.api.handlers.BTTokensManager;
import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.listener.RedeemListener;
import me.mraxetv.beastwithdraw.managers.assets.BeastLifeStealHandler;
import me.mraxetv.beastwithdraw.managers.assets.BeastMcMMORedeemHandler;
import me.mraxetv.beastwithdraw.managers.assets.BeastTokensHandler;
import me.mraxetv.beastwithdraw.managers.assets.CashNoteHandler;
import me.mraxetv.beastwithdraw.managers.assets.PlayerPointsHandler;
import me.mraxetv.beastwithdraw.managers.assets.XpBottleHandler;
import me.mraxetv.beastwithdraw.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class WithdrawManager {

    private BeastWithdrawPlugin pl;
    public static XpBottleHandler XP_BOTTLE = null;
    public static CashNoteHandler CASH_NOTE = null;
    public static BeastTokensHandler BEASTTOKENS_DEFAULT = null;
    public static PlayerPointsHandler PLAYERPOINTS_NOTE = null;
    public static BeastLifeStealHandler BEASTLIFESTEAL_NOTE = null;
    public static BeastMcMMORedeemHandler BEASTMCMMOREDEEM_NOTE = null;
    public static BeastMcMMORedeemHandler BEASTMCMMOREDEEM_SKILL_NOTE = null;



    private static HashMap<String, AssetHandler> assetsList;
    private static HashMap<String, String> assetAliases;
    private static HashMap<String, BeastTokensHandler> beastTokensHandlers;

    public WithdrawManager(BeastWithdrawPlugin pl) {
        this.pl = pl;
        initAssets();
    }

    public void initAssets() {
        assetsList = new HashMap<>();
        assetAliases = new HashMap<>();
        beastTokensHandlers = new HashMap<>();
        XP_BOTTLE = null;
        CASH_NOTE = null;
        BEASTTOKENS_DEFAULT = null;
        PLAYERPOINTS_NOTE = null;
        BEASTLIFESTEAL_NOTE = null;
        BEASTMCMMOREDEEM_NOTE = null;
        BEASTMCMMOREDEEM_SKILL_NOTE = null;

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

        if (isBeastTokensIntegrationEnabled()) {
            if ((pl.getServer().getPluginManager().isPluginEnabled("BeastTokens"))) {
                registerBeastTokensAssets();
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
        if (pl.getSettings().getBoolean("Settings.Withdraws.BeastMcMMORedeem.Enabled")) {
            if (pl.getServer().getPluginManager().isPluginEnabled("BeastMcMMORedeem")) {
                BEASTMCMMOREDEEM_NOTE = new BeastMcMMORedeemHandler(BeastWithdrawPlugin.getInstance(), "McMMORedeemCredits");
                if (BEASTMCMMOREDEEM_NOTE.isAvailable()) {
                    assetsList.put("mcmmoredeemcredits", BEASTMCMMOREDEEM_NOTE);
                    BEASTMCMMOREDEEM_SKILL_NOTE = BEASTMCMMOREDEEM_NOTE.getSkillHandler();
                    if (BEASTMCMMOREDEEM_SKILL_NOTE != null) {
                        assetsList.put("mcmmoredeemskillcredits", BEASTMCMMOREDEEM_SKILL_NOTE);
                        registerAssetAliases("mcmmoredeemskillcredits",
                                "mcmmoskillcredits",
                                "mcmmoskillnote",
                                "mcmmoskillnotes",
                                "mcmmoredeemskill",
                                "mcmmoredeemskills");
                        pl.getWithdrawLogger().prepareAssetLogs(BEASTMCMMOREDEEM_SKILL_NOTE);
                    }
                    pl.getWithdrawLogger().prepareAssetLogs(BEASTMCMMOREDEEM_NOTE);
                    registerAssetAliases("mcmmoredeemcredits",
                            "beastmcmmoredeem",
                            BEASTMCMMOREDEEM_NOTE.getCommandName(),
                            "mcmmocredits",
                            "mcmmoredeem",
                            "mcmmocredit",
                            "mcmmocreditsnote",
                            "mcmmoredeemcredits");
                    registerAssetAliases("mcmmoredeemcredits", BEASTMCMMOREDEEM_NOTE.getAliases());
                } else if (BEASTMCMMOREDEEM_NOTE.getWithdrawCMD() != null) {
                    BEASTMCMMOREDEEM_NOTE.getWithdrawCMD().unRegisterBukkitCommand();
                    BEASTMCMMOREDEEM_NOTE = null;
                    BeastWithdrawPlugin.getInstance().getUtils().sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cBeastMcMMORedeem is enabled, but its credit API is not available.");
                }
            } else {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cServer is missing 'BeastMcMMORedeem' plugin which you need for 'mcMMO Redeem Credits' notes to work!");
            }
        }
        syncCommandsLater();

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

        if (beastTokensHandlers != null) {
            beastTokensHandlers.clear();
        }

    }

    public void refreshBeastTokensAssets() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(pl, this::refreshBeastTokensAssets);
            return;
        }

        unregisterBeastTokensAssets();
        if (isBeastTokensIntegrationEnabled()) {
            if (pl.getServer().getPluginManager().isPluginEnabled("BeastTokens")) {
                registerBeastTokensAssets();
            } else {
                BeastWithdrawPlugin.getInstance().getUtils().sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cServer is missing 'BeastTokens' plugin which you need for 'Tokens Note' to work!");
            }
        }
        syncCommandsLater();
    }

    public BeastTokensHandler getBeastTokensHandler(String currencyId) {
        if (beastTokensHandlers == null || beastTokensHandlers.isEmpty()) return null;
        String normalized = normalizeLookup(currencyId);
        if (normalized == null) return BEASTTOKENS_DEFAULT;
        return beastTokensHandlers.get(normalized);
    }

    private void unregisterBeastTokensAssets() {
        BEASTTOKENS_DEFAULT = null;
        if (beastTokensHandlers != null) {
            beastTokensHandlers.clear();
        }
        if (assetsList == null || assetsList.isEmpty()) {
            return;
        }

        Set<String> removedAssetIds = new HashSet<>();
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, AssetHandler> entry : assetsList.entrySet()) {
            AssetHandler handler = entry.getValue();
            if (!(handler instanceof BeastTokensHandler)) continue;

            if (handler.getWithdrawCMD() != null) {
                handler.getWithdrawCMD().unRegisterBukkitCommand();
            }
            keysToRemove.add(entry.getKey());
            removedAssetIds.add(entry.getKey().toLowerCase(Locale.ENGLISH));
        }

        for (String key : keysToRemove) {
            assetsList.remove(key);
        }

        if (assetAliases != null && !removedAssetIds.isEmpty()) {
            assetAliases.entrySet().removeIf(entry -> removedAssetIds.contains(entry.getValue().toLowerCase(Locale.ENGLISH)));
        }
    }

    private void registerBeastTokensAssets() {
        Map<String, BTTokensManager> managers = getBeastTokensManagers();
        if (managers.isEmpty()) {
            BTTokensManager defaultManager = getDefaultBeastTokensManager();
            if (defaultManager != null) managers.put(resolveCurrencyId("Tokens", defaultManager), defaultManager);
        }

        if (managers.isEmpty()) {
            BeastWithdrawPlugin.getInstance().getUtils().sendMessage(pl.getServer().getConsoleSender(), "&4[" + pl.getDescription().getName() + "] &cBeastTokens is enabled, but no currency token managers were available for BeastWithdraw notes!");
            return;
        }

        String defaultCurrencyId = getDefaultCurrencyId();
        BeastTokensHandler firstHandler = null;

        for (Map.Entry<String, BTTokensManager> entry : managers.entrySet()) {
            BTTokensManager manager = entry.getValue();
            if (manager == null || isReadOnly(manager)) continue;

            String currencyId = resolveCurrencyId(entry.getKey(), manager);
            if (currencyId == null || currencyId.trim().isEmpty()) continue;
            if (!isBeastTokensCurrencyEnabled(currencyId)) continue;

            String assetId = toCurrencyAssetId(currencyId);
            if (assetId == null || assetId.trim().isEmpty()) continue;

            String commandId = toCurrencyWithdrawCommand(currencyId);
            if (commandId == null || commandId.trim().isEmpty()) continue;

            boolean defaultCurrency = defaultCurrencyId == null
                    ? firstHandler == null
                    : currencyId.equalsIgnoreCase(defaultCurrencyId);
            BeastTokensHandler handler = new BeastTokensHandler(pl, assetId, commandId, currencyId, manager, true);
            String key = assetId.toLowerCase(Locale.ENGLISH);

            assetsList.put(key, handler);
            registerAssetAliases(key, commandId);
            registerAssetAliases(key, handler.getAliases());
            beastTokensHandlers.put(currencyId.toLowerCase(Locale.ENGLISH), handler);
            beastTokensHandlers.put(key, handler);
            pl.getWithdrawLogger().prepareAssetLogs(handler);

            if (firstHandler == null) {
                firstHandler = handler;
            }
            if (defaultCurrency) {
                BEASTTOKENS_DEFAULT = handler;
            }
        }

        if (BEASTTOKENS_DEFAULT == null) {
            BEASTTOKENS_DEFAULT = firstHandler;
        }
    }

    private boolean isBeastTokensIntegrationEnabled() {
        return pl.getSettings().getBoolean("Settings.Withdraws.BeastTokens.Enabled", false);
    }

    private boolean isBeastTokensCurrencyEnabled(String currencyId) {
        String normalizedCurrencyId = stringOrNull(currencyId);
        if (normalizedCurrencyId == null) return true;

        String basePath = "Settings.Withdraws.BeastTokens.Currencies";
        Boolean override = getCurrencyEnabledOverride(basePath + ".Overrides", normalizedCurrencyId);
        if (override != null) return override;

        override = getCurrencyEnabledOverride(basePath, normalizedCurrencyId);
        if (override != null) return override;

        return pl.getSettings().getBoolean(basePath + ".DefaultEnabled", true);
    }

    private Boolean getCurrencyEnabledOverride(String sectionPath, String currencyId) {
        if (!pl.getSettings().isSection(sectionPath)) return null;

        Section section = pl.getSettings().getSection(sectionPath);
        for (Object rawKey : section.getKeys()) {
            String key = String.valueOf(rawKey);
            if (key.equalsIgnoreCase("DefaultEnabled") || key.equalsIgnoreCase("Overrides")) continue;
            if (!key.equalsIgnoreCase(currencyId)) continue;

            String path = sectionPath + "." + key;
            if (pl.getSettings().isSection(path)) {
                String enabledPath = path + ".Enabled";
                return pl.getSettings().contains(enabledPath) ? pl.getSettings().getBoolean(enabledPath, true) : null;
            }
            return pl.getSettings().getBoolean(path, true);
        }
        return null;
    }

    private Map<String, BTTokensManager> getBeastTokensManagers() {
        Map<String, BTTokensManager> managers = new LinkedHashMap<>();
        Plugin beastTokens = Bukkit.getPluginManager().getPlugin("BeastTokens");
        if (beastTokens == null || !beastTokens.isEnabled()) return managers;

        Object value = invoke(beastTokens, "getCurrencyTokenManagers");
        if (!(value instanceof Map)) return managers;

        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (!(entry.getValue() instanceof BTTokensManager)) continue;
            String currencyId = resolveCurrencyId(entry.getKey() == null ? null : String.valueOf(entry.getKey()), (BTTokensManager) entry.getValue());
            if (currencyId == null || currencyId.trim().isEmpty()) continue;
            managers.put(currencyId, (BTTokensManager) entry.getValue());
        }
        return managers;
    }

    private BTTokensManager getDefaultBeastTokensManager() {
        try {
            return BeastTokensAPI.getTokensManager();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String getDefaultCurrencyId() {
        Plugin beastTokens = Bukkit.getPluginManager().getPlugin("BeastTokens");
        if (beastTokens == null || !beastTokens.isEnabled()) return null;

        Object settings = invoke(beastTokens, "getDefaultCurrencySettings");
        String id = stringOrNull(invoke(settings, "getId"));
        if (id != null) return id;

        BTTokensManager manager = getDefaultBeastTokensManager();
        return manager == null ? null : resolveCurrencyId(null, manager);
    }

    private String resolveCurrencyId(String fallback, BTTokensManager manager) {
        String id = stringOrNull(invoke(manager, "getCurrencyId"));
        if (id != null) return id;

        Object settings = invoke(manager, "getCurrencySettings");
        id = stringOrNull(invoke(settings, "getId"));
        if (id != null) return id;

        id = stringOrNull(fallback);
        return id == null ? "Tokens" : id;
    }

    private boolean isReadOnly(BTTokensManager manager) {
        Object value = invoke(manager, "isReadOnly");
        return value instanceof Boolean && (Boolean) value;
    }

    private String toCurrencyWithdrawCommand(String currencyId) {
        String normalized = toCurrencyAssetId(currencyId);
        if (normalized.isEmpty()) return null;
        return normalized + "withdraw";
    }

    private String toCurrencyAssetId(String currencyId) {
        if (currencyId == null) return "";
        return currencyId.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "");
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

    private void registerAssetAliases(String primaryId, List<String> aliases) {
        if (aliases == null) return;
        registerAssetAliases(primaryId, aliases.toArray(new String[0]));
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

    private String normalizeLookup(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        return normalized.isEmpty() ? null : normalized;
    }

    private Object invoke(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String stringOrNull(Object value) {
        if (value == null) return null;
        String string = String.valueOf(value).trim();
        return string.isEmpty() ? null : string;
    }

    private void syncCommandsLater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Bukkit.getServer().getClass().getMethod("syncCommands").invoke(Bukkit.getServer());
                } catch (Exception ignored) {
                }
            }
        }.runTaskLater(pl, 1);
    }

}

