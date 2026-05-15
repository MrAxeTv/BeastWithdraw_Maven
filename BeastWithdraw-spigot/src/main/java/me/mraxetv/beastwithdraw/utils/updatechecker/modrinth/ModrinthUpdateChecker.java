package me.mraxetv.beastwithdraw.utils.updatechecker.modrinth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.utils.updatechecker.UpdateHttpClient;
import org.bukkit.Bukkit;

import java.time.Instant;

public class ModrinthUpdateChecker {

    private final BeastWithdrawPlugin pl;
    private final String projectSlugOrId;

    private volatile boolean updateAvailable = false;
    private volatile String latestVersion = null;

    public ModrinthUpdateChecker(BeastWithdrawPlugin pl, String projectSlugOrId) {
        this.pl = pl;
        this.projectSlugOrId = projectSlugOrId;
    }

    public void checkOnceAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
            try {
                String url = "https://api.modrinth.com/v2/project/" + projectSlugOrId + "/version";
                String responseBody = UpdateHttpClient.get(url, pl.getName() + "/" + pl.getDescription().getVersion());
                if (responseBody == null || responseBody.trim().isEmpty()) return;

                JsonArray versions = new JsonParser().parse(responseBody).getAsJsonArray();
                if (versions.size() == 0) return;

                JsonObject latest = null;
                for (JsonElement element : versions) {
                    JsonObject candidate = element.getAsJsonObject();
                    if (latest == null) {
                        latest = candidate;
                        continue;
                    }

                    Instant latestInstant = Instant.parse(latest.get("date_published").getAsString());
                    Instant candidateInstant = Instant.parse(candidate.get("date_published").getAsString());
                    if (candidateInstant.isAfter(latestInstant)) {
                        latest = candidate;
                    }
                }

                if (latest == null) return;

                String newest = latest.get("version_number").getAsString();
                String current = pl.getDescription().getVersion();

                this.latestVersion = newest;
                this.updateAvailable = isNewer(newest, current);

            } catch (Exception ignored) {
            }
        });
    }

    // Returns true if "a" is strictly newer than "b" (e.g. 2.5.8 > 2.5.6)
    public static boolean isNewer(String a, String b) {
        if (a == null || b == null) return false;

        String[] as = a.trim().split("\\.");
        String[] bs = b.trim().split("\\.");

        int len = Math.max(as.length, bs.length);
        for (int i = 0; i < len; i++) {
            int ai = i < as.length ? parseIntSafe(as[i]) : 0;
            int bi = i < bs.length ? parseIntSafe(bs[i]) : 0;
            if (ai > bi) return true;
            if (ai < bi) return false;
        }
        return false; // equal
    }

    private static int parseIntSafe(String s) {
        try {
            String digits = s.replaceAll("[^0-9].*$", "").replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
