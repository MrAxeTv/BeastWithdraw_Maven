package me.mraxetv.beastwithdraw;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public class MyPlugin extends JavaPlugin {


    private Logger logger;


    @Override
    public void onEnable() {

        //new JavaLogger();

      //  Logger rootLogger = LogManager.getRootLogger();
        //rootLogger.atLevel(Level.OFF);

        String pluginName = getDescription().getName();
        String serverVersion = Bukkit.getServer().getVersion();
       // if (serverVersion.contains("Spigot")) {
        System.setProperty("beastPlugin", pluginName);
        LoggerContext context = new LoggerContext("logger2");
        context.setName(pluginName);

       InputStream stream = getResource("log4j3.xml");

        ConfigurationSource source = null;
        try {
            source = new ConfigurationSource(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Configuration config = ConfigurationFactory.getInstance().getConfiguration(context, source);

        context.start(config);
            logger = context.getLogger(pluginName);



        Logger loggerImpl = LogManager.getContext(false).getLogger("BeastWithdraw");
        System.out.println(loggerImpl.getLevel());

       // }else logger = LogManager.getLogger(pluginName);

        // Get the logger1 logger from the new context



        logger.debug("This is a debug message.");
        logger.info("This is an info message.");
        logger.warn("This is a warning message.");
        logger.error("This is an error message.");
        logger.fatal("This is a fatal message.");





    }


    public static boolean isSpigotServer() {
        try {
            Class.forName("org.bukkit.BukkitSpigot");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}


