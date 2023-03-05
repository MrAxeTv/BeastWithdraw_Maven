package me.mraxetv.beastwithdraw.extensions;


import org.apache.commons.lang.Validate;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class Extension extends URLClassLoader {

    public Extension(ExtensionLoader extensionLoader, final ClassLoader parent, PluginDescriptionFile description, final File dataFolder, final File file) throws IOException, InvalidPluginException, MalformedURLException {
        super(new URL[] {file.toURI().toURL()}, parent);
        Validate.notNull(extensionLoader, "Loader cannot be null");


    }

    //void onEnable();

    //void onDisable();

}
