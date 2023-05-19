package me.mraxetv.beastwithdraw.utils;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.apache.logging.log4j.LogManager;



import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BeastLogger {

   // private static final Logger logger = LogManager.getLogger(Bukkit.getName());


    public BeastLogger() {
        // Load Log4j2 configuration from XML file
        System.setProperty("log4j.configurationFile", "log4j2.xml");

        // Use the logger in your plugin code
        //logger.fatal("Plugin enabled!");
      //  logger.error("Plugin enabled!");
    }




}


/*public class BeastLogger {



   /* private static final Logger logger = LogManager.getLogger(BeastWithdrawPlugin.class);

    public BeastLogger(BeastWithdrawPlugin pl){

        test();

    }


    /*public void test() {
        // Initialize Log4j with the configuration file
        System.setProperty("log4j2.debug", "true");
        System.out.println("Log4j configuration file: " + System.getProperty("log4j.configurationFile"));
        System.setProperty("log4j.configurationFile", "log4j2.xml");

        // Log some messages at different levels
        logger.debug("Debug message");
        logger.info("Info message");
        logger.warn("Warning message");
        logger.error("Error message");
    }*/
//}

