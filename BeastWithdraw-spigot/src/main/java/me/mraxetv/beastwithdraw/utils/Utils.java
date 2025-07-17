package me.mraxetv.beastwithdraw.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import me.mraxetv.beastlib.utils.BUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

public class Utils extends BUtils
implements Listener {
	private BeastWithdrawPlugin pl;
	private String version;
	public static DecimalFormat df2;

	static {}

	public Utils(BeastWithdrawPlugin plugin) {
		super(plugin);
		this.pl = plugin;
		if(pl.getConfig().getBoolean("Settings.DisableDecimalAmounts")){
			df2 = new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

		}else df2 = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df2.setRoundingMode(RoundingMode.DOWN);
	}


	public static String getPrefix() {
		String prefix = ChatColor.translateAlternateColorCodes('&', BeastWithdrawPlugin.getInstance().getMessages().getString("Prefix"));
		return prefix;
	}

	public static String formatStackSize(String message, int amount) {
		if (amount <= 1) return message.replaceAll("%stack%", "");
		return message.replaceAll("%stack%", MessagesLang.STACK_SIZE.replaceAll("%amount%", "" + amount));
	}

	public void sendMessage(CommandSender sender , String message){
		if(sender instanceof Player) message = setPlaceholders((Player) sender,message);
		message = message.replaceAll("%prefix%",getPrefix());
		sender.sendMessage(setColor(message));
	}

	public void sendMessage(Player sender , String message){
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
		try {Double.parseDouble(value);}
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

