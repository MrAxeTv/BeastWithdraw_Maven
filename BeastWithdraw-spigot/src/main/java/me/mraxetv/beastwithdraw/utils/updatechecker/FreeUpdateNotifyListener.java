package me.mraxetv.beastwithdraw.utils.updatechecker;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.utils.updatechecker.modrinth.ModrinthUpdateChecker;
import me.mraxetv.beastwithdraw.utils.updatechecker.spigot.SpigotPlusUpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FreeUpdateNotifyListener implements Listener {

    private static final long JOIN_COOLDOWN_MS = 10_000L;
    private static final String ADMIN_JOIN_ENABLED_PATH = "Settings.UpdateNotifications.AdminJoin.Enabled";

    private final BeastWithdrawPlugin pl;
    private final FreeUpdateAnnouncer announcer;
    private final Map<UUID, Long> joinCooldown = new HashMap<>();

    public FreeUpdateNotifyListener(BeastWithdrawPlugin pl, FreeUpdateAnnouncer announcer) {
        this.pl = pl;
        this.announcer = announcer;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!pl.getSettings().getBoolean(ADMIN_JOIN_ENABLED_PATH, true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("BeastWithdraw.Admin")) return;
        if (isOnCooldown(player)) return;

        String current = pl.getDescription().getVersion();
        ModrinthUpdateChecker modrinth = announcer.getModrinth();
        SpigotPlusUpdateChecker plus = announcer.getBeastWithdrawPlus();

        if (modrinth.isUpdateAvailable()) {
            announcer.sendFreeCombinedPlayer(
                    player,
                    current,
                    modrinth.getLatestVersion(),
                    plus.getLatestVersion()
            );
            return;
        }

        if (plus.isUpdateAvailable()) {
            announcer.sendPremiumPromoPlayer(player, current, plus.getLatestVersion());
        }
    }

    private boolean isOnCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long last = joinCooldown.get(player.getUniqueId());
        if (last != null && now - last < JOIN_COOLDOWN_MS) {
            return true;
        }
        joinCooldown.put(player.getUniqueId(), now);
        return false;
    }
}
