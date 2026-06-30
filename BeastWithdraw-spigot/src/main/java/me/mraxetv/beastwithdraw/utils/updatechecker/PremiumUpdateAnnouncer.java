package me.mraxetv.beastwithdraw.utils.updatechecker;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.utils.updatechecker.spigot.SpigotPlusUpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PremiumUpdateAnnouncer extends AbstractUpdateAnnouncer {

    private final SpigotPlusUpdateChecker beastWithdrawPlus;

    public PremiumUpdateAnnouncer(BeastWithdrawPlugin pl) {
        super(pl);
        this.beastWithdrawPlus = new SpigotPlusUpdateChecker(pl, 130867);
    }

    @Override
    public void start() {
        beastWithdrawPlus.checkOnceAsync();

        addTask(Bukkit.getScheduler().runTaskTimer(pl,
                beastWithdrawPlus::checkOnceAsync,
                RECHECK_TICKS,
                RECHECK_TICKS));

        addTask(Bukkit.getScheduler().runTaskLater(pl, () -> {
            if (!beastWithdrawPlus.isUpdateAvailable()) return;

            String current = pl.getDescription().getVersion();
            sendPremiumUpdateConsole(current, beastWithdrawPlus.getLatestVersion());
        }, START_DELAY_TICKS));

        registerListener(new PremiumUpdateNotifyListener(pl, this));
    }

    public SpigotPlusUpdateChecker getBeastWithdrawPlus() {
        return beastWithdrawPlus;
    }

    public void sendPremiumUpdatePlayer(Player player, String current, String latestPremium) {
        if (latestPremium == null) return;

        p(player, border());
        p(player, title(RED + "PREMIUM UPDATE AVAILABLE"));
        p(player, G + "A newer premium build is available on " + BLUE + "SpigotMC" + G + ".");
        p(player, " ");

        p(player, section("+", "BeastWithdrawPlus"));
        p(player, bullet("Current", BAD + current));
        p(player, bullet("Latest", Y + latestPremium + G + " (Premium)"));

        p(player, " ");
        p(player, section(">", "DOWNLOAD"));
        p(player, BLUE + PREMIUM_LINK);

        p(player, border());
    }

    public void sendPremiumUpdateConsole(String current, String latestPremium) {
        if (latestPremium == null) return;

        c(border());
        c(title(RED + "PREMIUM UPDATE AVAILABLE"));
        c("%prefix% " + G + "A newer premium build is available on " + BLUE + "SpigotMC" + G + ".");
        c(" ");

        c(section("+", "BeastWithdrawPlus"));
        c("%prefix% " + bullet("Current", BAD + current));
        c("%prefix% " + bullet("Latest", Y + latestPremium + G + " (Premium)"));

        c(" ");
        c(section(">", "DOWNLOAD"));
        c("%prefix% " + BLUE + PREMIUM_LINK);

        c(border());
    }
}
