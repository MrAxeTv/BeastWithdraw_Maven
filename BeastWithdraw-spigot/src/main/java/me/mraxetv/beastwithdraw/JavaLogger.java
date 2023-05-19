package me.mraxetv.beastwithdraw;



import org.bukkit.Bukkit;

import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class JavaLogger {



    public JavaLogger() {




        Logger rootLogger = LogManager.getLogManager().getLogger("");
        System.out.println("Root Logger" + rootLogger.getName());
        //rootLogger.setLevel(Level.OFF);
        System.out.println("Root Logger" + rootLogger.getName());

        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.ALL);
        }
        Logger logger = Logger.getLogger("");
        for (Handler handler : logger.getHandlers()) {
            handler.setLevel(Level.ALL);
        }
        //LogManager.getLogManager().getLogger("Minecraft").setLevel(Level.OFF);
        Enumeration<String> e =  LogManager.getLogManager().getLoggerNames();
        while (e.hasMoreElements()){
            String element = e.nextElement();
            LogManager.getLogManager().getLogger("BeastWithdraw").info(element);
            LogManager.getLogManager().getLogger(element).setLevel(Level.OFF);
        }

    }
}
