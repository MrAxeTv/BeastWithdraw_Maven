package me.mraxetv.beastwithdraw.listener;


import me.mraxetv.beastcore.utils.nbtapi.NBTItem;
import me.mraxetv.beastcore.utils.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastwithdraw.events.BottleRedeemEvent;
import me.mraxetv.beastwithdraw.utils.XpManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;


public class XpBottleRedeemListener implements Listener {
	private BeastWithdrawPlugin pl;
	private Material material;
	

	public XpBottleRedeemListener(BeastWithdrawPlugin plugin) {
		pl = plugin;
		pl.getServer().getPluginManager().registerEvents(this, pl);
		material = (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1) ? Material.EXPERIENCE_BOTTLE : Material.valueOf("EXP_BOTTLE"));
	}

	
	@EventHandler(priority = EventPriority.LOWEST)
	public void mainHand(PlayerInteractEvent e) {
		if (!e.hasItem()) return;
		if (e.getItem().getType() == Material.AIR) return;
		if (!e.getItem().hasItemMeta()) return;
		if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		//if(e.getItem().getType() != material) return;
		NBTItem nbtItem = new NBTItem(e.getItem());
		if(!nbtItem.hasKey(pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.NBTLore"))) return;

		Player p = e.getPlayer();
		//Cancel dupe event on block click
		if(e.isCancelled() && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			e.setCancelled(true);
			return;
		}
		//Cancel First Time
		e.setCancelled(true);

		boolean offHand = false;

		if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_9_R1)) {
			if (e.getItem().equals(e.getPlayer().getInventory().getItemInOffHand())) {
				offHand = true;
			}
		}

		pl.getServer().getPluginManager().
				callEvent(new BottleRedeemEvent(e.getPlayer(),e.getItem(),nbtItem.getInteger(pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.NBTLore")),offHand));
		 return;

		
	}

	@EventHandler
	public void redeemEvent(BottleRedeemEvent e){
		Player p = e.getPlayer();

		if(e.isCancelled()) return;

		if(pl.getWithdrawManager().XP_BOTTLE.getConfig().getBoolean("Settings.AutoCollect")) {

			int xp  = XpManager.getTotalExperience(p);
			int receivedXp = e.getExp();

			XpManager.setTotalExperience(p,xp+receivedXp);

				String s = pl.getMessages().getString("Withdraws.XpBottle.Redeem");
				s = s.replaceAll("%received-amount%", ""+pl.getUtils().formatNumber(receivedXp));
				s = s.replaceAll("%balance%", ""+pl.getUtils().formatNumber(XpManager.getTotalExperience(p)));
				pl.getUtils().sendMessage(p,s);
			}
			else {
				ThrownExpBottle t = e.getPlayer().launchProjectile(ThrownExpBottle.class);
				t.setCustomName("XPB:"+e.getExp());

			}
			if(pl.getWithdrawManager().XP_BOTTLE.getConfig().getBoolean("Settings.Sounds.Redeem.Enabled")) {
				try {
					String sound = pl.getWithdrawManager().XP_BOTTLE.getConfig().getString("Settings.Sounds.Redeem.Sound");
					p.playSound(p.getLocation(), Sound.valueOf(sound), 1f, 1f);

				} catch (Exception e1) {
					Bukkit.getServer().getConsoleSender().sendMessage(pl.getUtils().getPrefix() +"§cBroken sound in XpBottle Redeem section!");
				}
			}
		if (e.getItem().getAmount() > 1) {
			e.getItem().setAmount(e.getItem().getAmount() - 1);
		}
		else if (e.inOffHand()) {
			p.getInventory().setItemInOffHand(null);
		} else {
			p.getInventory().removeItem(new ItemStack[] { e.getItem() });
		}
		p.updateInventory();

	}




	@EventHandler(priority = EventPriority.LOWEST)
	public void xpThrow(ExpBottleEvent e){
		if(e.getEntity().getCustomName() == null) return;
		if(!e.getEntity().getCustomName().startsWith("XPB:")) return;
		int xp = Integer.parseInt(e.getEntity().getCustomName().replaceAll("XPB:",""));
		e.setExperience(xp);
	}

	


   @EventHandler
	public void PlayerDeath(PlayerDeathEvent e) {
		if (!pl.getWithdrawManager().XP_BOTTLE.getConfig().getBoolean("Settings.DropOnDeath")) return;
		Player p = e.getEntity();
		
		if(!p.hasPermission("BeastWithdraw.XpBottle.Drop"))return;
		int xp = XpManager.getTotalExperience(p);
		if(xp <= 0) return;
		double dropPercentage = pl.getWithdrawManager().XP_BOTTLE.getConfig().getDouble("Settings.DropPercentage")/100;
		xp = (int) (xp * dropPercentage);

		ItemStack Xpb = pl.getItemManger().getXpb(p.getName(), xp, 1, true);

		p.getWorld().dropItem(p.getLocation(), Xpb);
		p.setTotalExperience(0);
		p.setLevel(0);
		p.setExp(0);
		e.setDroppedExp(0);
	}

}
