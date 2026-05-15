package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ItemDropListener implements Listener {

    private BeastWithdrawPlugin pl;

    public ItemDropListener(BeastWithdrawPlugin pl) {
        this.pl = pl;
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }


    @EventHandler
    public void itemDrop(PlayerDropItemEvent e) {

        if (!e.getItemDrop().getItemStack().hasItemMeta()) return;
        NBTItem nbtItem = new NBTItem(e.getItemDrop().getItemStack());
        if (!nbtItem.hasTag("RedeemType")) return;

        String type = nbtItem.getString("RedeemType").toLowerCase();
        AssetHandler assetHandler = pl.getWithdrawManager().getAssetHandler(type);

        if(assetHandler.getConfig().contains("Settings.CustomName")){
            String hologram = assetHandler.getConfig().getString("Settings.CustomName");
            hologram = hologram.replace("%amount%",assetHandler.formatWithPreSuffix(nbtItem.getDouble(assetHandler.getNbtTag())));

            hologram = pl.getUtils().setPlaceholders(e.getPlayer(), hologram);



            e.getItemDrop().setCustomName(hologram);
            e.getItemDrop().setCustomNameVisible(true);

            //Set glow color
            if(assetHandler.getConfig().getBoolean("Settings.Glow.Enabled") && assetHandler.getConfig().contains("Settings.Glow.Color"))
                setGlowColor( e.getItemDrop(),ChatColor.valueOf(assetHandler.getConfig().getString("Settings.Glow.Color")));

        }

    }
    public void setGlowColor(Item item, ChatColor color) {

        item.setGlowing(true); // Makes the item glow

        // Add to team to color the glow
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "glow_" + color.name().toLowerCase();

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setColor(color);
        }

        String entry = item.getUniqueId().toString();
        team.addEntry(entry);
    }

}
