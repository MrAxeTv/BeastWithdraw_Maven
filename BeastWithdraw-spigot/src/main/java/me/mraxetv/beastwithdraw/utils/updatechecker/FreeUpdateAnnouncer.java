package me.mraxetv.beastwithdraw.utils.updatechecker;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.utils.updatechecker.modrinth.ModrinthUpdateChecker;
import me.mraxetv.beastwithdraw.utils.updatechecker.spigot.SpigotPlusUpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class FreeUpdateAnnouncer extends AbstractUpdateAnnouncer {

    private static final long STARTUP_MESSAGE_DELAY_TICKS = 1L;

    private final ModrinthUpdateChecker modrinth;
    private final SpigotPlusUpdateChecker beastWithdrawPlus;

    public FreeUpdateAnnouncer(BeastWithdrawPlugin pl) {
        super(pl);
        this.modrinth = new ModrinthUpdateChecker(pl, "beastwithdraw");
        this.beastWithdrawPlus = new SpigotPlusUpdateChecker(pl, 130867);
    }

    @Override
    public void start() {
        addTask(Bukkit.getScheduler().runTaskLater(pl, () -> Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
            modrinth.checkOnce();
            beastWithdrawPlus.checkOnce();
            Bukkit.getScheduler().runTask(pl, this::sendStartupConsoleMessage);
        }), STARTUP_MESSAGE_DELAY_TICKS));

        addTask(Bukkit.getScheduler().runTaskTimer(pl, () -> {
            modrinth.checkOnceAsync();
            beastWithdrawPlus.checkOnceAsync();
        }, RECHECK_TICKS, RECHECK_TICKS));

        registerListener(new FreeUpdateNotifyListener(pl, this));
    }

    private void sendStartupConsoleMessage() {
        String current = pl.getDescription().getVersion();

        if (modrinth.isUpdateAvailable()) {
            sendFreeCombinedConsole(current, modrinth.getLatestVersion(), beastWithdrawPlus.getLatestVersion());
            return;
        }

        if (beastWithdrawPlus.isUpdateAvailable()) {
            sendPremiumPromoConsole(current, beastWithdrawPlus.getLatestVersion());
        }
    }

    public ModrinthUpdateChecker getModrinth() {
        return modrinth;
    }

    public SpigotPlusUpdateChecker getBeastWithdrawPlus() {
        return beastWithdrawPlus;
    }

    public void sendFreeCombinedPlayer(Player player, String current, String latestFree, String latestPremium) {
        if (latestFree == null) return;

        p(player, border());
        p(player, title(RED + "FREE UPDATE AVAILABLE"));
        p(player, G + "A newer free build is available on " + BLUE + "Modrinth" + G + ".");
        p(player, " ");

        p(player, section("*", "FREE"));
        p(player, bullet("Current", BAD + current));
        p(player, bullet("Latest", OK + latestFree));

        if (latestPremium != null) {
            p(player, " ");
            p(player, section("+", "BeastWithdrawPlus"));
            p(player, bullet("Latest", Y + latestPremium + G + " (Premium)"));
            p(player, bullet("Includes", G + "premium updates + premium support"));
        }

        p(player, " ");
        p(player, section(">", "LINKS"));
        p(player, BLUE + "- " + W + "Free: " + BLUE + FREE_LINK);
        if (latestPremium != null) {
            p(player, BLUE + "- " + W + "Premium: " + BLUE + PREMIUM_LINK);
        }

        p(player, border());
    }

    public void sendFreeCombinedConsole(String current, String latestFree, String latestPremium) {
        if (latestFree == null) return;

        c(border());
        c(title(RED + "FREE UPDATE AVAILABLE"));
        c("%prefix% " + G + "A newer free build is available on " + BLUE + "Modrinth" + G + ".");
        c(" ");

        c(section("*", "FREE"));
        c("%prefix% " + bullet("Current", BAD + current));
        c("%prefix% " + bullet("Latest", OK + latestFree));

        if (latestPremium != null) {
            c(" ");
            c(section("+", "BeastWithdrawPlus"));
            c("%prefix% " + bullet("Latest", Y + latestPremium + G + " (Premium)"));
            c("%prefix% " + bullet("Includes", G + "premium updates + premium support"));
        }

        c(" ");
        c(section(">", "LINKS"));
        c("%prefix% " + BLUE + "- " + W + "Free: " + BLUE + FREE_LINK);
        if (latestPremium != null) {
            c("%prefix% " + BLUE + "- " + W + "Premium: " + BLUE + PREMIUM_LINK);
        }

        c(border());
    }

    public void sendPremiumPromoPlayer(Player player, String current, String latestPremium) {
        if (latestPremium == null) return;

        p(player, border());
        p(player, title(RED + "PREMIUM AVAILABLE"));
        p(player, G + "You are on the " + OK + "latest FREE" + G + " version.");
        p(player, G + "Premium gives " + Y + "earlier updates" + G + " + " + Y + "premium support" + G + ".");
        p(player, " ");

        p(player, section("+", "BeastWithdrawPlus"));
        p(player, bullet("Current", BAD + current));
        p(player, bullet("Latest", Y + latestPremium + G + " (Premium)"));

        p(player, " ");
        p(player, section(">", "GET PREMIUM"));
        p(player, BLUE + PREMIUM_LINK);

        p(player, border());
    }

    public void sendPremiumPromoConsole(String current, String latestPremium) {
        if (latestPremium == null) return;

        c(border());
        c(title(RED + "PREMIUM AVAILABLE"));
        c("%prefix% " + G + "You are on the " + OK + "latest FREE" + G + " version.");
        c("%prefix% " + G + "Premium gives " + Y + "earlier updates" + G + " + " + Y + "premium support" + G + ".");
        c(" ");

        c(section("+", "BeastWithdrawPlus"));
        c("%prefix% " + bullet("Current", BAD + current));
        c("%prefix% " + bullet("Latest", Y + latestPremium + G + " (Premium)"));

        c(" ");
        c(section(">", "GET PREMIUM"));
        c("%prefix% " + BLUE + PREMIUM_LINK);

        c(border());
    }
}
