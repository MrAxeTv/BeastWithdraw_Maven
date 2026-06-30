package me.mraxetv.beastwithdraw.utils.updatechecker;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractUpdateAnnouncer implements UpdateAnnouncer {

    public static final String FREE_LINK = "https://modrinth.com/plugin/beastwithdraw";
    public static final String PREMIUM_LINK = "https://www.spigotmc.org/resources/beastwithdraw-plus-multy-withdraw.130867/";

    protected static final long START_DELAY_TICKS = 20L * 3L;
    protected static final long RECHECK_TICKS = 20L * 60L * 10L;

    protected static final String RED = "&4&l";
    protected static final String BLUE = "&b&l";
    protected static final String D = "&8";
    protected static final String G = "&7";
    protected static final String W = "&f";
    protected static final String OK = "&a";
    protected static final String BAD = "&c";
    protected static final String Y = "&e";

    protected final BeastWithdrawPlugin pl;

    private final List<BukkitTask> tasks = new ArrayList<>();
    private Listener listener;

    protected AbstractUpdateAnnouncer(BeastWithdrawPlugin pl) {
        this.pl = pl;
    }

    @Override
    public void shutdown() {
        for (BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
    }

    protected void addTask(BukkitTask task) {
        tasks.add(task);
    }

    protected void registerListener(Listener listener) {
        this.listener = listener;
        pl.getServer().getPluginManager().registerEvents(listener, pl);
    }

    protected void p(Player player, String message) {
        pl.getUtils().sendMessage(player, message);
    }

    protected void c(String message) {
        pl.getUtils().sendMessage(pl.getServer().getConsoleSender(), message);
    }

    protected String border() {
        return D + "&m------------------------------";
    }

    protected String title(String right) {
        return BLUE + "* " + W + "%prefix% " + D + "- " + right;
    }

    protected String section(String marker, String name) {
        return BLUE + marker + " " + RED + name;
    }

    protected String bullet(String key, String value) {
        return BLUE + "> " + G + key + ": " + value;
    }
}
