package me.mraxetv.beastwithdraw.utils;

import java.text.NumberFormat;
import java.util.Locale;

import me.mraxetv.beastlib.utils.BUtils;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;

public class Utils extends BUtils
implements Listener {
	private BeastWithdrawPlugin pl;
	private String version;


	static {}

	public Utils(BeastWithdrawPlugin plugin) {
		super(plugin);
		this.pl = plugin;

	}


	public String getPrefix() {
		return BeastWithdrawPlugin.getInstance().getMessages().getString("Prefix");

	}

	public static String formatStackSize(String message, int amount) {
		if (amount <= 1) return message.replace("%stack%", "");
		return message.replace("%stack%", MessagesLang.STACK_SIZE.replace("%amount%", "" + amount));
	}



	public void sendMessage(CommandSender sender, String message) {

		super.sendMessage(sender, message);
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

	public String setPlaceholders(CommandSender sender, String s) {
		s = s.replace("%prefix%", getPrefix());
		if(sender instanceof Player){
		s = s.replace("%player%", sender.getName());
		}
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

