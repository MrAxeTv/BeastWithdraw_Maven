package me.mraxetv.beastwithdraw;

import me.mraxetv.beastcore.utils.nbtapi.utils.MinecraftVersion;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class YmlConfig_Old {

	private File customConfigFile;
	private FileConfiguration customConfig;
	private BeastWithdrawPlugin pl;
	private String name ;


	public YmlConfig_Old(BeastWithdrawPlugin pl, String name) {
		this.pl= pl;
		this.name = name;
		saveDeafultConfig();
		configUpdate();

	}

	public FileConfiguration getConfig() {
		return this.customConfig;
	}

	public void saveDeafultConfig() {
		customConfigFile = new File(pl.getDataFolder(), name);
		if (!customConfigFile.exists()) {
			customConfigFile.getParentFile().mkdirs();
			//Getting proper version of file 1.8 or 1.13+
            File temp;  
			if(MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1)) {
            	  pl.saveResource("configs\\1.13\\"+name, false);
            	  temp = new File(pl.getDataFolder()+"/configs/1.13/"+name);
            	  temp.renameTo(customConfigFile);  
              }else {
            	  pl.saveResource("configs\\1.8\\"+name, false);
            	  temp = new File(pl.getDataFolder()+"/configs/1.8/"+name);
            	  temp.renameTo(customConfigFile);       	      	  
              }
          deleteDirectory( new File(pl.getDataFolder()+"/configs"));

		}
         //Loading File
		customConfig= new YamlConfiguration();
		try {
			customConfig.load(new InputStreamReader(new FileInputStream(customConfigFile), "UTF-8"));
			//customConfig.load(customConfigFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//Deleting not needed copies 
	boolean deleteDirectory(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}
	private void configUpdate() {
		InputStreamReader newConfigStream = null;
		YamlConfiguration newConfig = null;
		try {
			if(MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1)) {
				newConfigStream = new InputStreamReader(pl.getResource("configs/1.13/"+name), "UTF-8");

			}else {
				newConfigStream = new InputStreamReader(pl.getResource("configs/1.8/"+name), "UTF-8");
			}
			if (newConfigStream != null) {
				newConfig = YamlConfiguration.loadConfiguration((Reader)newConfigStream);
			}
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		double configVersion = newConfig.getDouble("ConfigVersion");
		double oldVersion = customConfig.getDouble("ConfigVersion");


		if(customConfig.getDouble("ConfigVersion") < configVersion || !customConfig.isSet("ConfigVersion")) {
			File file;
			DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HH.mm.ss");
			Date date = new Date();
			//System.out.println(dateFormat.format(date)); //2016/11/16 12:08:43

			file = new File(pl.getDataFolder(), "config.yml");
			boolean renameResult = file.renameTo(new File(pl.getDataFolder(), "config_"+ dateFormat.format(date) + "_old.yml"));
			if(renameResult) {
				saveDeafultConfig();
				pl.getServer().getConsoleSender().sendMessage(
						ChatColor.translateAlternateColorCodes('&',"&7[&4Beast&bWithdraw&7] &cOld "+name+" ("+ oldVersion +") has been replaced with new version " + configVersion));

			}else {
				pl.getServer().getConsoleSender().sendMessage(
						ChatColor.translateAlternateColorCodes('&',"&7[&4Beast&bWithdraw&7] &cServer has failed to replace old version of "+
						name + " please contact author MrAxeTv!"));
			}
		}
	}



}
