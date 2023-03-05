package me.mraxetv.beastwithdraw.extensions;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.plugin.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.Bukkit.getName;

public class ExtensionManager {

    private BeastWithdrawPlugin pl;
    private HashMap<String, Extension> extensionsList;
    private final Map<Pattern, ExtensionLoader> fileAssociations = new HashMap<Pattern, ExtensionLoader>();


    public ExtensionManager(BeastWithdrawPlugin pl) {
        this.pl = pl;
        extensionsList = new HashMap<>();

        ExtensionLoader extensionLoader = new ExtensionLoader(pl);


        Pattern[] patterns = extensionLoader.getExtensionFileFilters();

        synchronized (this) {
            for (Pattern pattern : patterns) {
                fileAssociations.put(pattern, extensionLoader);
            }
        }


        File extensionsFolder = new File(pl.getDataFolder() + "/Extensions");

        if (!extensionsFolder.exists()) extensionsFolder.mkdir();

        loadExtensions(extensionsFolder);


    }


    public void loadExtensions(File directory) {
        Validate.notNull(directory, "Directory cannot be null");
        Validate.isTrue(directory.isDirectory(), "Directory must be a directory");


        Set<Pattern> filters = fileAssociations.keySet();

        for (File file : directory.listFiles()) {
            ExtensionLoader loader = null;
            for (Pattern filter : filters) {
                Matcher match = filter.matcher(file.getName());
                if (match.find()) {
                    loader = fileAssociations.get(filter);
                    Bukkit.broadcastMessage(file.getName());
                }
            }

            if (loader == null) continue;


            PluginDescriptionFile description = null;


            try {
                description = loader.getExtensionsDescription(file);

                Bukkit.broadcastMessage("Dec " + description.getMain());

            } catch (InvalidDescriptionException e) {
                e.printStackTrace();
            }


        }


    }

    public synchronized Extension loadExtension(File file) throws InvalidPluginException, UnknownDependencyException {
        Validate.notNull(file, "File cannot be null");


        Set<Pattern> filters = fileAssociations.keySet();
        Plugin result = null;

        for (Pattern filter : filters) {
            String name = file.getName();
            Matcher match = filter.matcher(name);

            if (match.find()) {
                ExtensionLoader loader = fileAssociations.get(filter);

                result = loader.loadPlugin(file);
            }
        }

        if (result != null) {
            //plugins.add(result);
            //lookupNames.put(result.getDescription().getName(), result);
            for (String provided : result.getDescription().getProvides()) {
                //lookupNames.putIfAbsent(provided, result);
            }
        }

        //return result;
        return null;
    }



}
