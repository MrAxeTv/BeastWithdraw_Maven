package me.mraxetv.beastwithdraw.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

public class Utils
implements Listener {
	private BeastWithdrawPlugin pl;
	private String version;
	private final static Pattern PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
	public static DecimalFormat df2 = new DecimalFormat("#.###");

	static {
		df2.setRoundingMode(RoundingMode.DOWN);
	}

	public Utils(BeastWithdrawPlugin plugin) {
		this.pl = plugin;
	}


	public static String getPrefix() {
		String prefix = ChatColor.translateAlternateColorCodes('&', BeastWithdrawPlugin.getInstance().getMessages().getString("Prefix"));
		return prefix;
	}

	public static String setColor(String s) {
		s = setHexColors(s);
		return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
	}

	public static String setHexColors(String s) {
		if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R1)) return s;
		Matcher matcher = PATTERN.matcher(s);
		while (matcher.find()) {
			String c = s.substring(matcher.start(), matcher.end());

			s = s.replace(c, "" + net.md_5.bungee.api.ChatColor.of(c.replace("&", "")));
			matcher = PATTERN.matcher(s);
		}
		return s;
	}


	public static String setAmount(int amount){

		if (amount == 1) return "";
		return ConfigUtils.AMOUNT.replaceAll("%amount%",""+amount);
	}

	public static void sendMessage(CommandSender sender , String message){
		message = message.replaceAll("%prefix%",getPrefix());
		sender.sendMessage(setColor(message));
	}

	public static void sendMessage(Player sender , String message){
		sender.sendMessage(setPlaceholders(sender,message));
	}

	public void sendLog(String s) {
		s = ChatColor.translateAlternateColorCodes('&', s);
		pl.getServer().getConsoleSender().sendMessage(getPrefix()+s);
		
	}

	public boolean fullInv(Player p) {
		int check = p.getInventory().firstEmpty();
		if (check == -1) {
			return true;
		}
		return false;
	}

	public static boolean isInt(String value) {
		try {
			Integer.parseInt(value);
		}
		catch (Exception efr) {
			return false;
		}
		return true;
	}
	public static boolean isDouble(String value) {
		try {
		 Double.parseDouble(value);
		}
		catch (Exception efr) {
			return false;
		}
		return true;
	}

	public static String setPlaceholders(Player p, String s) {
		s = s.replaceAll("%prefix%", getPrefix());
		s = s.replaceAll("%player%", p.getName());
		//s = s.replaceAll("%balance%", formatNumber(XpManager.getTotalExperience(p)));
		s = setColor(s);
		return s;
	}


	public void noPermission(Player p) {
		sendMessage(p,pl.getMessages().getString("Withdraws.NoPermission"));
	}
    public static String formatDouble(double number) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(number);
    }
    public static String formatNumber(int number) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(number);
    }


	public static void addItem(Player p, ItemStack item){

		if(item.getAmount() < 65){

			p.getInventory().addItem(item);
			return;
		}
		int stacks = item.getAmount()/64;
		int leftovers = item.getAmount()%64;

		item.setAmount(64);
		for(int i = 0; i < stacks; i++){
			if(p.getInventory().firstEmpty() != -1){
				p.getInventory().addItem(item);
			}
			else{p.getLocation().getWorld().dropItemNaturally(p.getLocation(),item);}
		}
		//Give leftovers of item stack!
		item.setAmount(leftovers);
		if(p.getInventory().firstEmpty() != -1){
			p.getInventory().addItem(item);
		}
		else{p.getLocation().getWorld().dropItemNaturally(p.getLocation(),item);}

	}

}

