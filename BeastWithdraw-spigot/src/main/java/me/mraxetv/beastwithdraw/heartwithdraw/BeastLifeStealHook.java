package me.mraxetv.beastwithdraw.heartwithdraw;

import me.mraxetv.beastlifesteal.BeastLifeSteal;
import me.mraxetv.beastlifesteal.data.HeartChangeReason;
import me.mraxetv.beastlifesteal.elimination.EliminationService;
import me.mraxetv.beastlifesteal.heart.HeartService;
import me.mraxetv.beastlib.lib.boostedyaml.block.implementation.Section;
import org.bukkit.entity.Player;

public final class BeastLifeStealHook {


    public boolean isAvailable() {
        return getPlugin() != null;
    }

    public int getStoredHearts(Player player) {
        HeartService heartService = getHeartService();
        return heartService != null ? heartService.getHearts(player) : 0;
    }

    public boolean wouldWithdrawEliminate(Player player, int amount) {
        return getStoredHearts(player) - amount <= getMinHearts();
    }

    public boolean removeHeartsForWithdraw(Player player, int amount) {
        if (amount <= 0 || getStoredHearts(player) < amount) {
            return false;
        }

        boolean lethalWithdraw = wouldWithdrawEliminate(player, amount);
        HeartService heartService = getHeartService();
        if (heartService == null) {
            return false;
        }
        heartService.removeHearts(player, amount, HeartChangeReason.ADMIN_REMOVE);

        if (lethalWithdraw) {
            EliminationService eliminationService = getEliminationService();
            if (eliminationService != null) {
                eliminationService.eliminate(player, null);
            }
        }

        return true;
    }

    public boolean addHeartsFromRedeem(Player player, int amount) {
        if (amount <= 0) {
            return false;
        }

        HeartService heartService = getHeartService();
        if (heartService == null) {
            return false;
        }

        heartService.addHearts(player, amount, HeartChangeReason.ADMIN_ADD);
        return true;
    }

    public int getMaxHearts(Player player) {
        BeastLifeSteal plugin = getPlugin();
        if (plugin == null) {
            return 0;
        }

        String override = getProfileOverride(plugin, player, "MaxHearts");
        if (!isBlank(override)) {
            try {
                return Integer.parseInt(override);
            } catch (NumberFormatException ignored) {
            }
        }

        return plugin.getCfg().getInt("Heart.Settings.MaxHearts", 20);
    }

    public int getRedeemableCapacity(Player player) {
        return Math.max(0, getMaxHearts(player) - getStoredHearts(player));
    }

    public boolean isGracePeriodActive() {
        // BeastLifeSteal 2.0.0 does not expose runtime grace period state via a public API.
        return false;
    }

    private HeartService getHeartService() {
        BeastLifeSteal plugin = getPlugin();
        return plugin != null ? plugin.getServices().get(HeartService.class) : null;
    }

    private EliminationService getEliminationService() {
        BeastLifeSteal plugin = getPlugin();
        return plugin != null ? plugin.getServices().get(EliminationService.class) : null;
    }

    private int getMinHearts() {
        BeastLifeSteal plugin = getPlugin();
        if (plugin == null) {
            return 0;
        }

        try {
            return plugin.getCfg().getInt("Heart.Settings.MinHearts", 0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String getProfileOverride(BeastLifeSteal plugin, Player player, String key) {
        String profilesPath = "Heart.Settings.PermissionProfiles";
        if (player == null || !plugin.getCfg().isSection(profilesPath)) {
            return null;
        }

        Section profiles = plugin.getCfg().getSection(profilesPath);
        int bestPriority = Integer.MIN_VALUE;
        String bestValue = null;

        for (Object rawKey : profiles.getKeys()) {
            String profileId = String.valueOf(rawKey);
            String base = profilesPath + "." + profileId + ".";
            String permission = plugin.getCfg().getString(base + "Permission", "");
            if (isBlank(permission) || !player.hasPermission(permission)) {
                continue;
            }

            String value = plugin.getCfg().getString(base + key, null);
            if (isBlank(value)) {
                continue;
            }

            int priority = plugin.getCfg().getInt(base + "Priority", 0);
            if (bestValue == null || priority > bestPriority) {
                bestPriority = priority;
                bestValue = value;
            }
        }

        return bestValue;
    }

    private BeastLifeSteal getPlugin() {
        return BeastLifeSteal.getInstance();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
