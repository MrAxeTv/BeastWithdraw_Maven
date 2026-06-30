package me.mraxetv.beastwithdraw.gui.withdraw;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.Sound;

import java.util.Locale;

final class WithdrawGuiAnimationSettings {
    enum Type {
        SLIDE_LEFT_TO_RIGHT,
        SLIDE_RIGHT_TO_LEFT,
        SNAKE,
        DIAGONAL_WAVE,
        CENTER_OUT,
        SPIRAL,
        RANDOM_POP,
        RAIN;

        static Type fromString(String value, Type fallback) {
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }

            try {
                return valueOf(value.trim().toUpperCase(Locale.ENGLISH)
                        .replace('-', '_')
                        .replace(' ', '_'));
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }

    enum Menu {
        MAIN_MENU("Triggers.MainMenu"),
        MCMMO_REDEEM_SKILL_MENU("Triggers.McMMORedeemSkillMenu"),
        AMOUNT_MENU("Triggers.AmountMenu");

        private final String triggerPath;

        Menu(String triggerPath) {
            this.triggerPath = triggerPath;
        }

        private String triggerPath(String base) {
            return base + "." + triggerPath;
        }
    }

    private final boolean enabled;
    private final Type type;
    private final boolean triggerMainMenu;
    private final boolean triggerMcMMORedeemSkillMenu;
    private final boolean triggerAmountMenu;
    private final int startDelayTicks;
    private final int tickDelay;
    private final int itemsPerTick;
    private final int rowPauseTicks;
    private final boolean animateEmptySlots;
    private final boolean lockClicksUntilFinished;
    private final boolean placeholderItemEnabled;
    private final boolean soundEnabled;
    private final Sound startSound;
    private final Sound itemSound;
    private final Sound rowSound;
    private final Sound finishSound;
    private final float startVolume;
    private final float startPitch;
    private final float itemVolume;
    private final float itemPitch;
    private final float itemPitchStep;
    private final float itemPitchMax;
    private final float rowVolume;
    private final float rowPitch;
    private final float finishVolume;
    private final float finishPitch;
    private final int itemSoundEvery;

    private WithdrawGuiAnimationSettings(
            boolean enabled,
            Type type,
            boolean triggerMainMenu,
            boolean triggerMcMMORedeemSkillMenu,
            boolean triggerAmountMenu,
            int startDelayTicks,
            int tickDelay,
            int itemsPerTick,
            int rowPauseTicks,
            boolean animateEmptySlots,
            boolean lockClicksUntilFinished,
            boolean placeholderItemEnabled,
            boolean soundEnabled,
            Sound startSound,
            Sound itemSound,
            Sound rowSound,
            Sound finishSound,
            float startVolume,
            float startPitch,
            float itemVolume,
            float itemPitch,
            float itemPitchStep,
            float itemPitchMax,
            float rowVolume,
            float rowPitch,
            float finishVolume,
            float finishPitch,
            int itemSoundEvery) {
        this.enabled = enabled;
        this.type = type;
        this.triggerMainMenu = triggerMainMenu;
        this.triggerMcMMORedeemSkillMenu = triggerMcMMORedeemSkillMenu;
        this.triggerAmountMenu = triggerAmountMenu;
        this.startDelayTicks = startDelayTicks;
        this.tickDelay = tickDelay;
        this.itemsPerTick = itemsPerTick;
        this.rowPauseTicks = rowPauseTicks;
        this.animateEmptySlots = animateEmptySlots;
        this.lockClicksUntilFinished = lockClicksUntilFinished;
        this.placeholderItemEnabled = placeholderItemEnabled;
        this.soundEnabled = soundEnabled;
        this.startSound = startSound;
        this.itemSound = itemSound;
        this.rowSound = rowSound;
        this.finishSound = finishSound;
        this.startVolume = startVolume;
        this.startPitch = startPitch;
        this.itemVolume = itemVolume;
        this.itemPitch = itemPitch;
        this.itemPitchStep = itemPitchStep;
        this.itemPitchMax = itemPitchMax;
        this.rowVolume = rowVolume;
        this.rowPitch = rowPitch;
        this.finishVolume = finishVolume;
        this.finishPitch = finishPitch;
        this.itemSoundEvery = itemSoundEvery;
    }

    static WithdrawGuiAnimationSettings from(BeastWithdrawPlugin plugin, WithdrawGuiProfile profile) {
        String base = "Animations.Open";
        Sound defaultItemSound = WithdrawGuiSoundSettings.resolveSound(plugin, "UI_BUTTON_CLICK", null,
                "withdraw-settings.yml " + base + ".Sounds.Item");

        return new WithdrawGuiAnimationSettings(
                profile.getBoolean(base + ".Enabled", false),
                Type.fromString(profile.getString(base + ".Type", "CENTER_OUT"), Type.CENTER_OUT),
                profile.getBoolean(Menu.MAIN_MENU.triggerPath(base), true),
                profile.getBoolean(Menu.MCMMO_REDEEM_SKILL_MENU.triggerPath(base), true),
                profile.getBoolean(Menu.AMOUNT_MENU.triggerPath(base), true),
                Math.max(0, profile.getInt(base + ".StartDelayTicks", 0)),
                Math.max(1, profile.getInt(base + ".TickDelay", 1)),
                Math.max(1, profile.getInt(base + ".ItemsPerTick", 1)),
                Math.max(0, profile.getInt(base + ".RowPauseTicks", 1)),
                profile.getBoolean(base + ".AnimateEmptySlots", true),
                profile.getBoolean(base + ".LockClicksUntilFinished", true),
                profile.getBoolean(base + ".PlaceholderItem.Enabled", true),
                profile.getBoolean(base + ".Sounds.Enabled", true),
                resolveConfiguredSound(plugin, profile, base + ".Sounds.Start", null),
                resolveConfiguredSound(plugin, profile, base + ".Sounds.Item", defaultItemSound),
                resolveConfiguredSound(plugin, profile, base + ".Sounds.Row", null),
                resolveConfiguredSound(plugin, profile, base + ".Sounds.Finish",
                        WithdrawGuiSoundSettings.resolveSound(plugin, "ENTITY_EXPERIENCE_ORB_PICKUP", null,
                                "withdraw-settings.yml " + base + ".Sounds.Finish")),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.StartVolume", 0.6D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.StartPitch", 0.8D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.ItemVolume", 0.45D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.ItemPitch", 1.25D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.ItemPitchStep", 0.03D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.ItemPitchMax", 2.0D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.RowVolume", 0.5D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.RowPitch", 1.45D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.FinishVolume", 0.7D)),
                Math.max(0F, (float) profile.getDouble(base + ".Sounds.FinishPitch", 1.0D)),
                Math.max(1, profile.getInt(base + ".Sounds.ItemEvery", 1)));
    }

    private static Sound resolveConfiguredSound(BeastWithdrawPlugin plugin, WithdrawGuiProfile profile,
                                                String path, Sound fallback) {
        if (!profile.contains(path)) {
            return fallback;
        }
        String soundId = profile.getString(path, null);
        if (soundId == null || soundId.trim().isEmpty()) {
            return null;
        }
        return WithdrawGuiSoundSettings.resolveSound(plugin, soundId, fallback,
                "withdraw-settings.yml " + path);
    }

    boolean shouldAnimate(Menu menu) {
        if (!enabled) {
            return false;
        }
        if (menu == null) {
            return false;
        }
        switch (menu) {
            case MAIN_MENU:
                return triggerMainMenu;
            case MCMMO_REDEEM_SKILL_MENU:
                return triggerMcMMORedeemSkillMenu;
            case AMOUNT_MENU:
                return triggerAmountMenu;
            default:
                return false;
        }
    }

    Type getType() {
        return type;
    }

    int getStartDelayTicks() {
        return startDelayTicks;
    }

    int getTickDelay() {
        return tickDelay;
    }

    int getItemsPerTick() {
        return itemsPerTick;
    }

    int getRowPauseTicks() {
        return rowPauseTicks;
    }

    boolean isAnimateEmptySlots() {
        return animateEmptySlots;
    }

    boolean isLockClicksUntilFinished() {
        return lockClicksUntilFinished;
    }

    boolean isPlaceholderItemEnabled() {
        return placeholderItemEnabled;
    }

    boolean isSoundEnabled() {
        return soundEnabled;
    }

    Sound getStartSound() {
        return startSound;
    }

    Sound getItemSound() {
        return itemSound;
    }

    Sound getRowSound() {
        return rowSound;
    }

    Sound getFinishSound() {
        return finishSound;
    }

    float getStartVolume() {
        return startVolume;
    }

    float getStartPitch() {
        return startPitch;
    }

    float getItemVolume() {
        return itemVolume;
    }

    float getItemPitch() {
        return itemPitch;
    }

    float getItemPitchStep() {
        return itemPitchStep;
    }

    float getItemPitchMax() {
        return itemPitchMax;
    }

    float getRowVolume() {
        return rowVolume;
    }

    float getRowPitch() {
        return rowPitch;
    }

    float getFinishVolume() {
        return finishVolume;
    }

    float getFinishPitch() {
        return finishPitch;
    }

    int getItemSoundEvery() {
        return itemSoundEvery;
    }
}
