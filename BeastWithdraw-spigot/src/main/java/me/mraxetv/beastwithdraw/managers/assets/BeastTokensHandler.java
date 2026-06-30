package me.mraxetv.beastwithdraw.managers.assets;


import lombok.Getter;
import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beasttokens.api.BeastTokensAPI;
import me.mraxetv.beasttokens.api.handlers.BTTokensManager;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.commands.tokenwithdraw.BeastTokenNoteCMD;
import me.mraxetv.beastwithdraw.events.BTokensRedeemEvent;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BeastTokensHandler extends AssetHandler<Double> {
    private static final String BEASTTOKENS_CURRENCY_NBT = "BeastTokensCurrency";
    private static final String BEASTTOKENS_TEMPLATE_FOLDER = "BeastTokens";
    private static final String BEASTTOKENS_TEMPLATE_RESOURCE = "BeastTokens.yml";

    private BTTokensManager api;
    private final String currencyId;
    private final String commandName;
    @Getter
    private BeastTokenNoteCMD beastTokenNoteCMD;
    private String currencyDisplayName;
    private String currencySymbol;
    private String currencyPrefix;
    private String currencyFormat;
    private String currencyNbtKey;

    public BeastTokensHandler(BeastWithdrawPlugin pl, String id) {
        this(pl, id, id, "Tokens", BeastTokensAPI.getTokensManager(), true);
    }

    public BeastTokensHandler(BeastWithdrawPlugin pl, String id, String currencyId, BTTokensManager manager, boolean registerCommand) {
        this(pl, id, id, currencyId, manager, registerCommand);
    }

    public BeastTokensHandler(BeastWithdrawPlugin pl, String id, String commandName, String currencyId, BTTokensManager manager, boolean registerCommand) {
        super(pl, id, getCurrencyFolderName(currencyId), "Withdraw.yml", BEASTTOKENS_TEMPLATE_FOLDER, "Withdraw.yml",
                "Withdraws/" + getCurrencyConfigFileName(currencyId),
                "Withdraws/" + getCurrencyFolderName(currencyId) + "/" + getCurrencyConfigFileName(currencyId),
                "Withdraws/" + BEASTTOKENS_TEMPLATE_RESOURCE,
                "Withdraws/" + BEASTTOKENS_TEMPLATE_FOLDER + "/" + BEASTTOKENS_TEMPLATE_RESOURCE);
        this.currencyId = currencyId == null || currencyId.trim().isEmpty() ? "Tokens" : currencyId.trim();
        this.commandName = commandName == null || commandName.trim().isEmpty() ? id : commandName.trim().toLowerCase(Locale.ENGLISH);
        this.api = manager == null ? getCurrencyManagerFromRuntime() : manager;
        readCurrencyMetadata();
        applyCurrencyDefaultsToConfig();

        if (registerCommand) {
            beastTokenNoteCMD = new BeastTokenNoteCMD(pl,this);
        }

        RedeemRegistry.register(id, BTokensRedeemEvent::new);
        if (!this.commandName.equalsIgnoreCase(id)) {
            RedeemRegistry.register(this.commandName, BTokensRedeemEvent::new);
        }
    }

    @Override
    public Double getBalance(Player p) {
        BTTokensManager manager = getApi();
        return manager == null ? 0.0D : manager.getTokens(p);
    }

    @Override
    protected void withdrawAmountExact(Player p, Double amount) {
        BTTokensManager manager = getApi();
        if (manager != null) manager.removeTokens(p,amount);
    }

    @Override
    protected void depositAmountExact(Player p, Double amount) {
        BTTokensManager manager = getApi();
        if (manager != null) manager.addTokens(p,amount);
    }

    @Override
    protected Double convertAmount(double amount) {
        return amount;
    }

    @Override
    public boolean isToBigAmount(double amount) {
        return false;
    }

    @Override
    public WithdrawCMD getWithdrawCMD() {
        return beastTokenNoteCMD;
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public List<String> getAliases() {
        Set<String> aliases = new LinkedHashSet<>();
        String commandId = getCommandName().toLowerCase(Locale.ENGLISH);

        for (String alias : getConfig().getStringList("Settings.CurrencyAliases")) {
            String value = replaceCurrencyPlaceholders(alias);
            if (value == null || value.trim().isEmpty()) continue;
            if (!value.equalsIgnoreCase(commandId)) aliases.add(value);
        }

        return new ArrayList<>(aliases);
    }

    @Override
    public ItemStack getItem(String owner, double value, int amount, boolean signed, double tax) {
        return getItem(owner, value, amount, signed, tax, null);
    }

    @Override
    public ItemStack getItem(String owner, double value, int amount, boolean signed, double tax, String amountOverrideId) {
        ItemStack itemStack = super.getItem(owner, value, amount, signed, tax, amountOverrideId);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                meta.setDisplayName(replaceCurrencyPlaceholders(meta.getDisplayName()));
            }
            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    lore.add(replaceCurrencyPlaceholders(line));
                }
                meta.setLore(lore);
            }
            itemStack.setItemMeta(meta);
        }

        NBTItem nbtItem = new NBTItem(itemStack);
        nbtItem.setString(BEASTTOKENS_CURRENCY_NBT, currencyId);
        return nbtItem.getItem();
    }

    @Override
    public String applyPlaceholders(String value, Player player) {
        return super.applyPlaceholders(replaceCurrencyPlaceholders(value), player);
    }

    @Override
    public boolean hasWithdrawPermission(Player player) {
        return hasPermission(player, "Withdraw");
    }

    @Override
    public boolean hasWithdrawAllPermission(Player player) {
        return hasPermission(player, "Withdraw.All");
    }

    @Override
    public boolean hasBypassFeePermission(Player player) {
        return hasPermission(player, "ByPass.Fee");
    }

    @Override
    public boolean hasBypassTaxPermission(Player player) {
        return hasPermission(player, "ByPass.Tax");
    }

    @Override
    public boolean hasPermissionNote(Player player, String permissionName) {
        return hasPermission(player, "PermissionNotes." + permissionName);
    }

    @Override
    public boolean hasRedeemPermission(Player player) {
        return hasPermission(player, "Redeem");
    }

    @Override
    public boolean hasStackedRedeemPermission(Player player) {
        return hasPermission(player, "Redeem.Stacked");
    }

    public String getCurrencyId() {
        return currencyId;
    }

    @Override
    public String getNbtTag() {
        return currencyNbtKey == null || currencyNbtKey.trim().isEmpty() ? currencyId : currencyNbtKey;
    }

    public BeastTokenNoteCMD getBeastTokenNoteCMD() {
        return beastTokenNoteCMD;
    }

    private boolean hasPermission(Player player, String suffix) {
        if (player == null || suffix == null) return false;
        if (player.hasPermission("BeastWithdraw." + getID() + "." + suffix)) return true;

        String commandId = getCommandName();
        return commandId != null
                && !commandId.equalsIgnoreCase(getID())
                && player.hasPermission("BeastWithdraw." + commandId + "." + suffix);
    }

    private BTTokensManager getApi() {
        BTTokensManager manager = getCurrencyManagerFromRuntime();
        if (manager != null) {
            api = manager;
            return manager;
        }
        if (hasRuntimeExactCurrencyLookup()) {
            api = null;
            return null;
        }
        if (api != null) return api;
        try {
            api = BeastTokensAPI.getTokensManager();
        } catch (Throwable ignored) {
            api = null;
        }
        return api;
    }

    private BTTokensManager getCurrencyManagerFromRuntime() {
        Object plugin = Bukkit.getPluginManager().getPlugin("BeastTokens");
        Object exactValue = invoke(plugin, "getCurrencyTokensManager", new Class[]{String.class}, new Object[]{currencyId});
        if (exactValue instanceof BTTokensManager) return (BTTokensManager) exactValue;
        if (hasRuntimeExactCurrencyLookup()) return null;

        try {
            Class<?> apiClass = Class.forName("me.mraxetv.beasttokens.api.BeastTokensAPI");
            Method method = apiClass.getMethod("getTokensManager", String.class);
            Object value = method.invoke(null, currencyId);
            if (value instanceof BTTokensManager) return (BTTokensManager) value;
        } catch (Throwable ignored) {
        }

        Object value = invoke(plugin, "getTokensManager", new Class[]{String.class}, new Object[]{currencyId});
        return value instanceof BTTokensManager ? (BTTokensManager) value : null;
    }

    private void readCurrencyMetadata() {
        currencyDisplayName = currencyId;
        currencySymbol = "";
        currencyPrefix = "";
        currencyFormat = "%symbol%%amount%";
        currencyNbtKey = currencyId;

        Object manager = getApi();
        if (manager == null) return;

        Object settings = invoke(manager, "getCurrencySettings");
        if (settings != null) {
            currencyDisplayName = stringOrDefault(invoke(settings, "getDisplayName"), currencyDisplayName);
            currencySymbol = stringOrDefault(invoke(settings, "getCurrencySymbol"), currencySymbol);
            currencyPrefix = stringOrDefault(invoke(settings, "getPrefix"), currencyPrefix);
            currencyFormat = normalizeCurrencyFormat(stringOrDefault(invoke(settings, "getFormat"), currencyFormat));
            Object noteSettings = invoke(settings, "getWithdrawSettings");
            currencyNbtKey = stringOrDefault(invoke(noteSettings, "getNbtKey"), currencyNbtKey);
            return;
        }

        currencySymbol = stringOrDefault(invoke(manager, "getSymbol"), currencySymbol);
    }

    private void applyCurrencyDefaultsToConfig() {
        boolean changed = false;
        if (getConfig().contains("Settings.NBTKey")) {
            getConfig().remove("Settings.NBTKey");
            changed = true;
        }

        String configuredSymbol = getConfig().getString("Settings.Messages.CurrencySymbol", null);
        if (isNewConfigFile() || configuredSymbol == null) {
            String symbol = currencySymbol == null ? "" : currencySymbol;
            getConfig().set("Settings.Messages.CurrencySymbol", symbol);
            changed = true;
        }

        String configuredFormat = getConfig().getString("Settings.Messages.Format", null);
        if (isNewConfigFile() || configuredFormat == null || configuredFormat.trim().isEmpty()) {
            getConfig().set("Settings.Messages.Format", normalizeCurrencyFormat(currencyFormat));
            changed = true;
        } else {
            String normalizedFormat = normalizeCurrencyFormat(configuredFormat);
            if (!normalizedFormat.equals(configuredFormat)) {
                getConfig().set("Settings.Messages.Format", normalizedFormat);
                changed = true;
            }
        }

        if (changed) saveConfig();
    }

    private String normalizeCurrencyFormat(String format) {
        if (format == null || format.trim().isEmpty()) return "%symbol%%amount%";
        String normalized = format.replace("%currency_raw%", "%amount_raw%")
                .replace("%currency%", "%amount%");
        if (normalized.trim().isEmpty()) return "%symbol%%amount%";
        return normalized;
    }

    private String replaceCurrencyPlaceholders(String value) {
        if (value == null) return null;

        String lower = currencyId.toLowerCase(Locale.ENGLISH);
        return value
                .replace("%currency_id%", currencyId)
                .replace("%currencyid%", currencyId)
                .replace("%currency_id_lower%", lower)
                .replace("%currency_name%", currencyDisplayName)
                .replace("%currency_display_name%", currencyDisplayName)
                .replace("%currency_symbol%", currencySymbol)
                .replace("%symbol%", currencySymbol)
                .replace("%currency_prefix%", currencyPrefix)
                .replace("%currency_withdraw_command%", getCommandName())
                .replace("%withdraw_command%", getCommandName());
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

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasMethod(Object target, String methodName, Class<?>... parameterTypes) {
        if (target == null) return false;
        try {
            target.getClass().getMethod(methodName, parameterTypes);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasRuntimeExactCurrencyLookup() {
        Object plugin = Bukkit.getPluginManager().getPlugin("BeastTokens");
        return hasMethod(plugin, "getCurrencyTokensManager", String.class);
    }

    private static String getCurrencyConfigFileName(String currencyId) {
        String id = currencyId == null || currencyId.trim().isEmpty() ? "Tokens" : currencyId.trim();
        String sanitized = id.replaceAll("[^A-Za-z0-9_-]", "");
        if (sanitized.isEmpty()) sanitized = "Tokens";
        return "BeastTokens-" + sanitized + ".yml";
    }

    private static String getCurrencyFolderName(String currencyId) {
        String id = currencyId == null || currencyId.trim().isEmpty() ? "Tokens" : currencyId.trim();
        String sanitized = id.replaceAll("[^A-Za-z0-9_-]", "");
        return sanitized.isEmpty() ? "Tokens" : sanitized;
    }

    private String stringOrDefault(Object value, String fallback) {
        if (value == null) return fallback;
        String string = String.valueOf(value);
        return string.trim().isEmpty() ? fallback : string;
    }
}
