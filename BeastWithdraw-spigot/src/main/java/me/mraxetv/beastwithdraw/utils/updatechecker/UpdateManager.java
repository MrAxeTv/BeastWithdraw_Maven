package me.mraxetv.beastwithdraw.utils.updatechecker;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

public class UpdateManager {

    public static final String FREE_LINK = AbstractUpdateAnnouncer.FREE_LINK;
    public static final String PREMIUM_LINK = AbstractUpdateAnnouncer.PREMIUM_LINK;

    private final UpdateAnnouncer announcer;

    public UpdateManager(BeastWithdrawPlugin pl) {
        this.announcer = pl.isPremiumBuild()
                ? new PremiumUpdateAnnouncer(pl)
                : new FreeUpdateAnnouncer(pl);
        this.announcer.start();
    }

    public void shutdown() {
        announcer.shutdown();
    }

    public UpdateAnnouncer getAnnouncer() {
        return announcer;
    }
}
