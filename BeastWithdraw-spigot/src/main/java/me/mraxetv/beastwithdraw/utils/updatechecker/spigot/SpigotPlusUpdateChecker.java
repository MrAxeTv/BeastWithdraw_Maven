package me.mraxetv.beastwithdraw.utils.updatechecker.spigot;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.utils.updatechecker.UpdateHttpClient;
import org.bukkit.Bukkit;

public class SpigotPlusUpdateChecker {

    private final BeastWithdrawPlugin pl;
    private final int resourceId;

    private volatile boolean updateAvailable = false;
    private volatile String latestVersion = null;

    public SpigotPlusUpdateChecker(BeastWithdrawPlugin pl, int resourceId) {
        this.pl = pl;
        this.resourceId = resourceId;
    }

    public void checkOnceAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(pl, this::checkOnce);
    }

    public void checkOnce() {
        try {
            String url = "https://api.spigotmc.org/legacy/update.php?resource=" + resourceId;
            String responseBody = UpdateHttpClient.get(url, pl.getName() + "/" + pl.getDescription().getVersion());
            if (responseBody == null) return;

            String newest = responseBody.trim();
            if (newest.isEmpty()) return;

            String current = pl.getDescription().getVersion();

            this.latestVersion = newest;
            this.updateAvailable = me.mraxetv.beastwithdraw.utils.updatechecker.modrinth.ModrinthUpdateChecker
                    .isNewer(newest, current);
        } catch (Exception ignored) {
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
