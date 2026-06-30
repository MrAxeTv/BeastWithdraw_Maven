package me.mraxetv.beastwithdraw.listener;

import me.mraxetv.beastlib.lib.nbtapi.NBTItem;
import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import me.mraxetv.beastwithdraw.managers.assets.BeastMcMMORedeemHandler;
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
        if (assetHandler == null) return;
        if (!nbtItem.hasKey(assetHandler.getNbtTag())) return;

        double amount = nbtItem.getDouble(assetHandler.getNbtTag());
        String amountOverrideId = assetHandler.getAmountOverrideId(e.getItemDrop().getItemStack());
        String customName = assetHandler.getCustomNameFor(amount, amountOverrideId);
        if(customName != null && !customName.trim().isEmpty()){
            String hologram;
            if (assetHandler instanceof BeastMcMMORedeemHandler) {
                BeastMcMMORedeemHandler mcMMOHandler = (BeastMcMMORedeemHandler) assetHandler;
                String skillName = mcMMOHandler.getSkillName(e.getItemDrop().getItemStack());
                if (skillName != null) {
                    hologram = assetHandler.applyRawNotePlaceholders(customName, e.getPlayer().getName(), amount, 0);
                    hologram = mcMMOHandler.applySkillPlaceholders(hologram, skillName, e.getPlayer());
                } else {
                    hologram = assetHandler.applyNotePlaceholders(customName, e.getPlayer().getName(), amount, 0);
                    hologram = assetHandler.applyPlaceholders(hologram, e.getPlayer());
                }
            } else {
                hologram = assetHandler.applyNotePlaceholders(customName, e.getPlayer().getName(), amount, 0);
                hologram = assetHandler.applyPlaceholders(hologram, e.getPlayer());
            }

            hologram = pl.getUtils().setPlaceholders(e.getPlayer(), hologram);



            e.getItemDrop().setCustomName(hologram);
            e.getItemDrop().setCustomNameVisible(true);

            //Set glow color
            String glowColor = assetHandler.getGlowColor(amount, amountOverrideId);
            if(assetHandler.isGlowEnabled(amount, amountOverrideId) && glowColor != null && !glowColor.trim().isEmpty()) {
                try {
                    setGlowColor(e.getItemDrop(), ChatColor.valueOf(glowColor.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }

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
