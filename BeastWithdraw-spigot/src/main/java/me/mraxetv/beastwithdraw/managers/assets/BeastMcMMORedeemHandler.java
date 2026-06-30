package me.mraxetv.beastwithdraw.managers.assets;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.commands.WithdrawCMD;
import me.mraxetv.beastwithdraw.commands.mcmmoredeem.McMMORedeemCreditNoteCMD;
import me.mraxetv.beastwithdraw.events.CustomRedeemEvent;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.redeem.RedeemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Method;
import java.util.Locale;

public class BeastMcMMORedeemHandler extends AssetHandler<Long> {
    private static final String API_CLASS = "me.mraxetv.beastmcmmoiredeem.api.BeastMcMMORedeemAPI";
    private static final String DEFAULT_COMMAND_NAME = "mcmmocreditwithdraw";
    private static final String DEFAULT_SKILL_NBT_KEY = "BeastMcMMORedeemSkill";
    private static final String SKILL_WITHDRAW_PERMISSION = "BeastWithdraw.McMMORedeemCredits.Withdraw.Skill";

    private McMMORedeemCreditNoteCMD mcMMORedeemCreditNoteCMD;
    private BeastMcMMORedeemHandler skillHandler;
    private final ThreadLocal<String> skillPlaceholderContext = new ThreadLocal<>();

    public BeastMcMMORedeemHandler(BeastWithdrawPlugin plugin, String id) {
        super(plugin, id, "BeastMcMMORedeem.yml");
        this.mcMMORedeemCreditNoteCMD = new McMMORedeemCreditNoteCMD(plugin, this);
        this.skillHandler = new BeastMcMMORedeemSkillHandler(plugin, "McMMORedeemSkillCredits");
        RedeemRegistry.register(id, CustomRedeemEvent::new);
        if (!getCommandName().equalsIgnoreCase(id)) {
            RedeemRegistry.register(getCommandName(), CustomRedeemEvent::new);
        }
    }

    protected BeastMcMMORedeemHandler(BeastWithdrawPlugin plugin, String id, String withdrawFileName, String depositFileName) {
        super(plugin, id, "BeastMcMMORedeem", withdrawFileName, depositFileName,
                "BeastMcMMORedeem", withdrawFileName, depositFileName, new String[0]);
        this.mcMMORedeemCreditNoteCMD = null;
        this.skillHandler = null;
        RedeemRegistry.register(id, CustomRedeemEvent::new);
    }

    public McMMORedeemCreditNoteCMD getMcMMORedeemCreditNoteCMD() {
        return mcMMORedeemCreditNoteCMD;
    }

    public BeastMcMMORedeemHandler getSkillHandler() {
        return skillHandler;
    }

    public boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BeastMcMMORedeem");
        return plugin != null && plugin.isEnabled() && invokeStaticBoolean("isAvailable");
    }

    public boolean isSkillNotesEnabled() {
        BeastMcMMORedeemHandler handler = getSkillConfigHandler();
        return handler.getConfig().getBoolean("Settings.Enabled", false) && invokeStaticBoolean("isSkillNoteExchangeEnabled");
    }

    public boolean hasSkillWithdrawPermission(Player player) {
        if (player == null) {
            return false;
        }

        return player.hasPermission(SKILL_WITHDRAW_PERMISSION);
    }

    public String normalizeSkillName(String skillName) {
        Object result = invokeStatic("normalizeSkillName", new Class[]{String.class}, new Object[]{skillName});
        if (result == null) {
            return null;
        }

        String normalized = String.valueOf(result).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public boolean isValidSkill(String skillName) {
        Object result = invokeStatic("isValidSkillForBeastWithdraw", new Class[]{String.class}, new Object[]{skillName});
        return result instanceof Boolean && ((Boolean) result).booleanValue();
    }

    public List<String> getSkillSuggestions() {
        Object result = invokeStatic("getEnabledSkillNames", new Class[0], new Object[0]);
        if (!(result instanceof String[])) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        Collections.addAll(values, (String[]) result);
        return values;
    }

    public long getRedeemableCapacity(Player player) {
        if (player == null) {
            return 0L;
        }

        Object result = invokeStatic("getRedeemableCapacity", new Class[]{OfflinePlayer.class}, new Object[]{player});
        return asLong(result);
    }

    public long getSkillBalance(Player player, String skillName) {
        if (player == null || skillName == null) {
            return 0L;
        }

        Object result = invokeStatic("getSkillCredits", new Class[]{Player.class, String.class}, new Object[]{player, skillName});
        return asLong(result);
    }

    public boolean withdrawSkillAmount(Player player, String skillName, long amount) {
        if (player == null || skillName == null || amount <= 0L) {
            return false;
        }

        Object result = invokeStatic("withdrawSkillCreditsForBeastWithdraw",
                new Class[]{Player.class, String.class, long.class},
                new Object[]{player, skillName, Long.valueOf(amount)});
        return result instanceof Boolean && ((Boolean) result).booleanValue();
    }

    public boolean depositSkillAmount(Player player, String skillName, long amount) {
        if (player == null || skillName == null || amount <= 0L) {
            return false;
        }

        Object result = invokeStatic("redeemSkillCreditsFromBeastWithdraw",
                new Class[]{Player.class, String.class, long.class},
                new Object[]{player, skillName, Long.valueOf(amount)});
        return result instanceof Boolean && ((Boolean) result).booleanValue();
    }

    public ItemStack getSkillItem(String owner, String skillName, double value, int amount, boolean signed, double tax, String amountOverrideId) {
        String normalizedSkill = normalizeSkillName(skillName);
        if (normalizedSkill != null) {
            skillPlaceholderContext.set(normalizedSkill);
        }
        ItemStack itemStack;
        try {
            itemStack = getItem(owner, value, amount, signed, tax, amountOverrideId);
        } finally {
            skillPlaceholderContext.remove();
        }
        if (normalizedSkill == null) {
            return itemStack;
        }

        itemStack = applySkillText(itemStack, normalizedSkill);
        me.mraxetv.beastlib.lib.nbtapi.NBTItem nbtItem = new me.mraxetv.beastlib.lib.nbtapi.NBTItem(itemStack);
        nbtItem.setString(getSkillNbtKey(), normalizedSkill);
        return nbtItem.getItem();
    }

    public boolean isSkillNote(ItemStack itemStack) {
        return getSkillName(itemStack) != null;
    }

    public String getSkillName(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        me.mraxetv.beastlib.lib.nbtapi.NBTItem nbtItem = new me.mraxetv.beastlib.lib.nbtapi.NBTItem(itemStack);
        if (!nbtItem.hasKey(getSkillNbtKey())) {
            return null;
        }

        String skillName = nbtItem.getString(getSkillNbtKey());
        return skillName == null || skillName.trim().isEmpty() ? null : skillName.trim();
    }

    public String getSkillMessage(String key, String fallback) {
        BeastMcMMORedeemHandler handler = getSkillConfigHandler();
        String value = handler.getConfig().getString("Settings.Messages." + key, null);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public String applySkillPlaceholders(String value, String skillName, Player player) {
        if (value == null) {
            return null;
        }

        String normalizedSkill = skillName == null ? "" : skillName;
        String skillText = value
                .replace("%type%", normalizedSkill)
                .replace("%skill%", normalizedSkill)
                .replace("%TYPE%", normalizedSkill)
                .replace("%SKILL%", normalizedSkill);
        return applyPlaceholders(skillText, player);
    }

    @Override
    public Long getBalance(Player player) {
        return Long.valueOf(callGetCredits(player));
    }

    @Override
    protected void withdrawAmountExact(Player player, Long amount) {
        callExchange("takeCreditsForBeastWithdraw", player, amount == null ? 0L : amount.longValue());
    }

    @Override
    protected void depositAmountExact(Player player, Long amount) {
        callExchange("addCreditsFromBeastWithdraw", player, amount == null ? 0L : amount.longValue());
    }

    @Override
    protected Long convertAmount(double amount) {
        if (amount <= 0D) {
            return Long.valueOf(0L);
        }
        if (amount >= Long.MAX_VALUE) {
            return Long.valueOf(Long.MAX_VALUE);
        }
        return Long.valueOf((long) Math.floor(amount));
    }

    @Override
    public boolean isToBigAmount(double amount) {
        return amount > Long.MAX_VALUE;
    }

    @Override
    public WithdrawCMD getWithdrawCMD() {
        return mcMMORedeemCreditNoteCMD;
    }

    @Override
    public String getCommandName() {
        String configured = getConfig().getString("Settings.CommandName", DEFAULT_COMMAND_NAME);
        return sanitizeCommand(configured, DEFAULT_COMMAND_NAME);
    }

    @Override
    public String applyPlaceholders(String value, Player player) {
        if (value == null) {
            return null;
        }

        return super.applyPlaceholders(value, player)
                .replace("%credits_name%", "mcMMO Redeem Credits")
                .replace("%credits_name_lower%", "mcMMO redeem credits")
                .replace("%credits_withdraw_command%", getCommandName())
                .replace("%withdraw_command%", getCommandName());
    }

    protected ItemStack applySkillText(ItemStack itemStack, String skillName) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return itemStack;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta.hasDisplayName()) {
            meta.setDisplayName(applySkillPlaceholders(meta.getDisplayName(), skillName, null));
        }
        if (meta.hasLore() && meta.getLore() != null) {
            List<String> lore = new ArrayList<>();
            for (String line : meta.getLore()) {
                lore.add(applySkillPlaceholders(line, skillName, null));
            }
            meta.setLore(lore);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    protected String getSkillNbtKey() {
        BeastMcMMORedeemHandler handler = getSkillConfigHandler();
        String configured = handler.getConfig().getString("Settings.SkillNBTKey",
                handler.getConfig().getString("Settings.NBTKey", DEFAULT_SKILL_NBT_KEY));
        return configured == null || configured.trim().isEmpty() ? DEFAULT_SKILL_NBT_KEY : configured.trim();
    }

    protected String getSkillPlaceholderContext() {
        return skillPlaceholderContext.get();
    }

    private BeastMcMMORedeemHandler getSkillConfigHandler() {
        return skillHandler == null ? this : skillHandler;
    }

    private String sanitizeCommand(String value, String fallbackValue) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
        normalized = normalized.replaceAll("[^a-z0-9_-]", "");
        if (!normalized.isEmpty()) {
            return normalized;
        }

        String fallbackCommand = fallbackValue == null ? "" : fallbackValue.trim().toLowerCase(Locale.ENGLISH);
        fallbackCommand = fallbackCommand.replaceAll("[^a-z0-9_-]", "");
        return fallbackCommand.isEmpty() ? DEFAULT_COMMAND_NAME : fallbackCommand;
    }

    private long callGetCredits(OfflinePlayer player) {
        if (player == null) {
            return 0L;
        }

        Object result = invokeStatic("getCredits", new Class[]{OfflinePlayer.class}, new Object[]{player});
        return asLong(result);
    }

    private long callExchange(String methodName, Player player, long amount) {
        if (player == null || amount <= 0L) {
            return callGetCredits(player);
        }

        Object result = invokeStatic(methodName, new Class[]{Player.class, long.class}, new Object[]{player, Long.valueOf(amount)});
        return asLong(result);
    }

    private boolean invokeStaticBoolean(String methodName) {
        Object value = invokeStatic(methodName, new Class[0], new Object[0]);
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    private Object invokeStatic(String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            Method method = apiClass.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number) {
            return Math.max(0L, ((Number) value).longValue());
        }
        return 0L;
    }
}
