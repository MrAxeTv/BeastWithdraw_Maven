package me.mraxetv.beastwithdraw.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class BeastUtils {

    private Plugin pl;
    private String rId;

    public BeastUtils(Plugin pl, String resourceId) {
        this.pl = pl;
        this.rId = resourceId;
    }

    public void getBVersion(final Consumer<String> konzumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.pl, () -> {
            try (InputStream iStr = new URL("htt"+"ps://ap"+"i.spi"+"gotmc.or"+"g/lega"+"cy/upd"+"ate.ph"+"p?resou"+"rce=" + this.rId).openStream(); Scanner sc = new Scanner(iStr)) {
                if (sc.hasNext()) {
                    konzumer.accept(sc.next());
                }
            } catch (IOException exception) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7[&4Beast&bTokens&7] &4Update check failed : " + exception.getMessage()));
            }
        });
    }

}
