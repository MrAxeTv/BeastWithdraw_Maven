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

public class UpdateNotifyListener implements Listener {

    private final BeastWithdrawPlugin pl;
    private final ModrinthUpdateChecker modrinth;
    private final SpigotPlusUpdateChecker plus;
    private final UpdateManager manager;

    private final Map<UUID, Long> joinCooldown = new HashMap<>();
    private static final long JOIN_COOLDOWN_MS = 10_000L;

    public UpdateNotifyListener(BeastWithdrawPlugin pl,
                                ModrinthUpdateChecker modrinth,
                                SpigotPlusUpdateChecker plus,
                                UpdateManager manager) {
        this.pl = pl;
        this.modrinth = modrinth;
        this.plus = plus;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // ✅ Only admins
        if (!p.hasPermission("BeastWithdraw.Admin")) return;

        // Cooldown (prevents relog spam)
        long now = System.currentTimeMillis();
        Long last = joinCooldown.get(p.getUniqueId());
        if (last != null && now - last < JOIN_COOLDOWN_MS) return;
        joinCooldown.put(p.getUniqueId(), now);

        String current = pl.getDescription().getVersion();

        // 1) Free outdated -> send free update message (DISABLEABLE on join)
        if (modrinth != null && modrinth.isUpdateAvailable()) {
            //if (!pl.getConfig().getBoolean("Options.UpdateNotifications")) return;

            manager.sendFreeCombinedPlayer(
                    p,
                    current,
                    modrinth.getLatestVersion(),
                    plus != null ? plus.getLatestVersion() : null
            );
            return;
        }

        // 2) Free is latest -> premium promo (NOT disableable)
        if (plus != null && plus.isUpdateAvailable()) {
            manager.sendPremiumPlayer(p, current, plus.getLatestVersion());
        }
    }
}