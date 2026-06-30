package me.mraxetv.beastwithdraw.gui.depositor;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class DepositGuiSoundSettings {
    private static final Map<String, String[]> SOUND_COMPAT_ALIASES = createSoundCompatAliases();
    private static final Set<String> LOGGED_SOUND_MESSAGES = Collections.synchronizedSet(new HashSet<String>());

    private final boolean enabled;
    private final Sound clickSound;
    private final Sound menuOpenSound;
    private final float volume;
    private final float pitch;

    private DepositGuiSoundSettings(boolean enabled, Sound clickSound, Sound menuOpenSound,
                                    float volume, float pitch) {
        this.enabled = enabled;
        this.clickSound = clickSound;
        this.menuOpenSound = menuOpenSound;
        this.volume = volume;
        this.pitch = pitch;
    }

    static DepositGuiSoundSettings from(BeastWithdrawPlugin plugin, DepositGuiProfile profile) {
        Sound defaultClick = resolveSound(plugin, "UI_BUTTON_CLICK", null, "deposit-settings.yml Sounds.Click");
        Sound defaultOpen = resolveSound(plugin, "ENTITY_VILLAGER_TRADE", null, "deposit-settings.yml Sounds.MenuOpen");
        return new DepositGuiSoundSettings(
                profile.getBoolean("Sounds.Enabled", true),
                resolveSound(plugin, profile.getString("Sounds.Click", "UI_BUTTON_CLICK"), defaultClick,
                        "deposit-settings.yml Sounds.Click"),
                resolveSound(plugin, profile.getString("Sounds.MenuOpen", "ENTITY_VILLAGER_TRADE"), defaultOpen,
                        "deposit-settings.yml Sounds.MenuOpen"),
                Math.max(0F, (float) profile.getDouble("Sounds.Volume", 1D)),
                Math.max(0F, (float) profile.getDouble("Sounds.Pitch", 1D))
        );
    }

    void playClick(Player player) {
        play(player, clickSound, volume, pitch);
    }

    void playMenuOpen(Player player) {
        play(player, menuOpenSound, volume, pitch);
    }

    private void play(Player player, Sound sound, float soundVolume, float soundPitch) {
        if (!enabled || player == null || sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, soundVolume, soundPitch);
    }

    static Sound resolveSound(BeastWithdrawPlugin plugin, String soundId, Sound fallback, String path) {
        if (soundId == null || soundId.trim().isEmpty()) {
            return null;
        }

        String normalized = soundId.trim().toUpperCase(Locale.ENGLISH);
        Sound sound = getSoundByName(normalized);
        if (sound != null) {
            return sound;
        }

        String[] aliases = SOUND_COMPAT_ALIASES.get(normalized);
        if (aliases != null) {
            for (String alias : aliases) {
                sound = getSoundByName(alias);
                if (sound != null) {
                    logSoundMessage(plugin, "Sound id '" + soundId + "' is not available at '" + path
                            + "'. Using '" + sound.name() + "' instead.");
                    return sound;
                }
            }
        }

        logSoundMessage(plugin, "Wrong or unsupported sound id '" + soundId + "' at '" + path + "'.");
        return fallback;
    }

    private static Sound getSoundByName(String soundId) {
        try {
            return Sound.valueOf(soundId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void logSoundMessage(BeastWithdrawPlugin plugin, String message) {
        if (plugin != null && LOGGED_SOUND_MESSAGES.add(message)) {
            plugin.getLogger().warning(message);
        }
    }

    private static Map<String, String[]> createSoundCompatAliases() {
        Map<String, String[]> aliases = new HashMap<>();
        aliases.put("UI_BUTTON_CLICK", new String[]{"CLICK"});
        aliases.put("ENTITY_VILLAGER_TRADE", new String[]{"VILLAGER_HAGGLE", "VILLAGER_YES"});
        aliases.put("ENTITY_VILLAGER_NO", new String[]{"VILLAGER_NO"});
        aliases.put("ENTITY_EXPERIENCE_ORB_PICKUP", new String[]{"ORB_PICKUP"});
        aliases.put("ENTITY_PLAYER_LEVELUP", new String[]{"LEVEL_UP"});
        aliases.put("BLOCK_NOTE_BLOCK_PLING", new String[]{"NOTE_PLING"});
        return aliases;
    }
}
