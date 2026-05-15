package me.mraxetv.beastwithdraw.utils.updatechecker;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.utils.updatechecker.modrinth.ModrinthUpdateChecker;
import me.mraxetv.beastwithdraw.utils.updatechecker.spigot.SpigotPlusUpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * UpdateManager
 * - FREE update check (Modrinth: beastwithdraw)
 * - PREMIUM update check (Spigot: BeastWithdrawPlus resource 130867)
 *
 * Messaging goals:
 * - Clean, non-wrapping layout in chat
 * - Uses BeastWithdraw brand colors: &4&l (dark red) and &b&l (light blue)
 * - Prefix is always visible and not shoved into long borders
 */
public class UpdateManager {

    private final BeastWithdrawPlugin pl;

    // FREE updates (Modrinth)
    private final ModrinthUpdateChecker modrinth;

    // PREMIUM updates (Spigot - BeastWithdrawPlus)
    private final SpigotPlusUpdateChecker beastWithdrawPlus;

    // Links
    public static final String FREE_LINK = "https://modrinth.com/plugin/beastwithdraw";
    public static final String PREMIUM_LINK = "https://www.spigotmc.org/resources/beastwithdraw-plus-multy-withdraw.130867/";

    // Timings
    private static final long START_DELAY_TICKS = 20L * 3L;        // 3s after startup
    private static final long CONSOLE_REPEAT_TICKS = 20L * 60 *10L;    // 1 min
    private static final long ADMIN_REPEAT_TICKS = 20L * 60L * 10L; // 5 min
    private static final long RECHECK_TICKS = 20L * 60L * 10L;     // 10 min

    // Brand colors (requested)
    private static final String RED = "&4&l";
    private static final String BLUE = "&b&l";
    private static final String D = "&8";
    private static final String G = "&7";
    private static final String W = "&f";
    private static final String OK = "&a";
    private static final String BAD = "&c";
    private static final String Y = "&e";

    public UpdateManager(BeastWithdrawPlugin pl) {
        this.pl = pl;

        this.modrinth = new ModrinthUpdateChecker(pl, "beastwithdraw");
        this.beastWithdrawPlus = new SpigotPlusUpdateChecker(pl, 130867);

        // Initial check
        modrinth.checkOnceAsync();
        beastWithdrawPlus.checkOnceAsync();

        // Re-check periodically (no reboot needed)
        Bukkit.getScheduler().runTaskTimerAsynchronously(pl, () -> {
            modrinth.checkOnceAsync();
            beastWithdrawPlus.checkOnceAsync();
        }, RECHECK_TICKS, RECHECK_TICKS);

        // Console reminder every 1 minute
        Bukkit.getScheduler().runTaskTimerAsynchronously(pl, () -> {
            String current = pl.getDescription().getVersion();

            // FREE outdated -> combined message (free + premium latest)
            if (modrinth.isUpdateAvailable()) {
                sendFreeCombinedConsole(current, modrinth.getLatestVersion(), beastWithdrawPlus.getLatestVersion());
                return; // don't promote premium-only until free is updated
            }

            // FREE latest -> premium promo if premium newer
            if (beastWithdrawPlus.isUpdateAvailable()) {
                sendPremiumConsole(current, beastWithdrawPlus.getLatestVersion());
            }

        }, START_DELAY_TICKS, CONSOLE_REPEAT_TICKS);

        // Admin in-game reminder every 5 minutes (premium only, only if free is latest)
        // Admin in-game reminder every 5 minutes (FREE or PREMIUM)
        Bukkit.getScheduler().runTaskTimerAsynchronously(pl, () -> {

            String current = pl.getDescription().getVersion();

            // 1) FREE update available -> notify admins (and include premium latest if you want)
            if (modrinth.isUpdateAvailable()) {

                String latestFree = modrinth.getLatestVersion();
                if (latestFree == null) return;

                String latestPremium = beastWithdrawPlus.getLatestVersion(); // can be null

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.hasPermission("BeastWithdraw.Admin")) continue;

                    // Your existing free message method (combined shows both links)
                    sendFreeCombinedPlayer(p, current, latestFree, latestPremium);
                }

                return; // important: don't also show premium-only spam
            }

            // 2) FREE is latest -> if premium is newer, notify admins
            if (beastWithdrawPlus.isUpdateAvailable()) {

                String latestPremium = beastWithdrawPlus.getLatestVersion();
                if (latestPremium == null) return;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.hasPermission("BeastWithdraw.Admin")) continue;

                    sendPremiumPlayer(p, current, latestPremium);
                }
            }

        }, START_DELAY_TICKS, ADMIN_REPEAT_TICKS);

        // Join listener (free join msg is disableable; premium isn't)
        pl.getServer().getPluginManager().registerEvents(
                new UpdateNotifyListener(pl, modrinth, beastWithdrawPlus, this),
                pl
        );
    }

    // =========================================================================
    // Internal send helpers (ALWAYS use your Utils)
    // =========================================================================

    private void p(Player p, String msg) {
        pl.getUtils().sendMessage(p, msg);
    }

    private void c(String msg) {
        pl.getUtils().sendMessage(pl.getServer().getConsoleSender(), msg);
    }

    // Short safe border (won't wrap)
    private String border() {
        return D + "&m------------------------------";
    }

    // Clean title line (no huge header)
    private String title(String right) {
        // Example: [BeastWithdraw]  •  PREMIUM AVAILABLE
        return BLUE + "❖ " + W + "%prefix% " + D + "• " + right;
    }

    // Small section header
    private String sec(String icon, String name) {
        return BLUE + icon + " " + RED + name;
    }

    // Bullet line
    private String bullet(String key, String value) {
        return BLUE + "» " + G + key + ": " + value;
    }

    // =========================================================================
    // FREE update messages (combined with premium info)
    // =========================================================================

    public void sendFreeCombinedPlayer(Player p, String current, String latestFree, String latestPremium) {
        if (latestFree == null) return;

        p(p, border());
        p(p, title(RED + "FREE UPDATE AVAILABLE"));
        p(p, G + "A newer free build is available on " + BLUE + "Modrinth" + G + ".");
        p(p, " ");

        p(p, sec("📦", "FREE"));
        p(p, bullet("Current", BAD + current));
        p(p, bullet("Latest", OK + latestFree));

        if (latestPremium != null) {
            p(p, " ");
            p(p, sec("⭐", "BeastWithdrawPlus"));
            p(p, bullet("Latest", Y + latestPremium + G + " (Premium)"));
            p(p, bullet("Includes", G + "premium updates + premium support"));
        }

        p(p, " ");
        p(p, sec("🔗", "LINKS"));
        p(p, BLUE + "• " + W + "Free: " + BLUE + FREE_LINK);
        if (latestPremium != null) {
            p(p, BLUE + "• " + W + "Premium: " + BLUE + PREMIUM_LINK);
        }

        p(p, border());
    }

    public void sendFreeCombinedConsole(String current, String latestFree, String latestPremium) {
        if (latestFree == null) return;

        c(border());
        c(title(RED + "FREE UPDATE AVAILABLE"));
        c("%prefix% " + G + "A newer free build is available on " + BLUE + "Modrinth" + G + ".");
        c(" ");

        c(sec("📦", "FREE"));
        c("%prefix% " + bullet("Current", BAD + current));
        c("%prefix% " + bullet("Latest", OK + latestFree));

        if (latestPremium != null) {
            c(" ");
            c(sec("⭐", "BeastWithdrawPlus"));
            c("%prefix% " + bullet("Latest", Y + latestPremium + G + " (Premium)"));
            c("%prefix% " + bullet("Includes", G + "premium updates + premium support"));
        }

        c(" ");
        c(sec("🔗", "LINKS"));
        c("%prefix% " + BLUE + "• " + W + "Free: " + BLUE + FREE_LINK);
        if (latestPremium != null) {
            c("%prefix% " + BLUE + "• " + W + "Premium: " + BLUE + PREMIUM_LINK);
        }

        c(border());
    }

    // =========================================================================
    // PREMIUM-only messages (only when FREE is already latest)
    // =========================================================================

    public void sendPremiumPlayer(Player p, String current, String latestPremium) {
        if (latestPremium == null) return;

        p(p, border());
        p(p, title(RED + "PREMIUM AVAILABLE"));
        p(p, G + "You are on the " + OK + "latest FREE" + G + " version.");
        p(p, G + "Premium gives " + Y + "earlier updates" + G + " + " + Y + "premium support" + G + ".");
        p(p, " ");

        p(p, sec("⭐", "BeastWithdrawPlus"));
        p(p, bullet("Current", BAD + current));
        p(p, bullet("Latest", Y + latestPremium + G + " (Premium)"));

        p(p, " ");
        p(p, sec("🔗", "GET PREMIUM"));
        p(p, BLUE + PREMIUM_LINK);

        p(p, border());
    }

    public void sendPremiumConsole(String current, String latestPremium) {
        if (latestPremium == null) return;

        c(border());
        c(title(RED + "PREMIUM AVAILABLE"));
        c("%prefix% " + G + "You are on the " + OK + "latest FREE" + G + " version.");
        c("%prefix% " + G + "Premium gives " + Y + "earlier updates" + G + " + " + Y + "premium support" + G + ".");
        c(" ");

        c(sec("⭐", "BeastWithdrawPlus"));
        c("%prefix% " + bullet("Current", BAD + current));
        c("%prefix% " + bullet("Latest", Y + latestPremium + G + " (Premium)"));

        c(" ");
        c(sec("🔗", "GET PREMIUM"));
        c("%prefix% " + BLUE + PREMIUM_LINK);

        c(border());
    }

    // =========================================================================
    // Optional getters
    // =========================================================================

    public ModrinthUpdateChecker getModrinth() {
        return modrinth;
    }

    public SpigotPlusUpdateChecker getBeastWithdrawPlus() {
        return beastWithdrawPlus;
    }

}
