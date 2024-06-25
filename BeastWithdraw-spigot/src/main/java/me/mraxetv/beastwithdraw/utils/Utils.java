package me.mraxetv.beastwithdraw.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import me.mraxetv.beastlib.lib.nbtapi.utils.MinecraftVersion;
import me.mraxetv.beastlib.utils.SUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

public class Utils extends SUtils
implements Listener {
	private BeastWithdrawPlugin pl;
	private String version;
	private final static Pattern PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
	public static DecimalFormat df2;


	static {


	}

	public Utils(BeastWithdrawPlugin plugin) {
		super(plugin);
		this.pl = plugin;
		if(pl.getConfig().getBoolean("Settings.DisableDecimalAmounts")){
			df2 = new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

		}else df2 = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df2.setRoundingMode(RoundingMode.DOWN);


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


	public static String setStackSize(int amount){

		if (amount == 1) return "";
		return ConfigLang.STACK_SIZE.replaceAll("%amount%",""+amount);
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

        return ConfigLang.NUMBER_FORMAT.format(number);
    }
    public static String formatNumber(int number) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(number);
    }

}

