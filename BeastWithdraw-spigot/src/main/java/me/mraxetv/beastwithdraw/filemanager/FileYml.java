package me.mraxetv.beastwithdraw.filemanager;

import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.api.yaml.YamlFileOptions;
import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class FileYml {
    private final JavaPlugin pl;
    private final String name;
    private final boolean autoUpdate;
    private final boolean newFile;
    private YamlDocument config;

    public FileYml(JavaPlugin plugin, String n) {
        this(plugin, n, true);
    }

    public FileYml(JavaPlugin plugin, String n, boolean autoUpdate) {
        this.pl = plugin;
        this.name = n;
        this.autoUpdate = autoUpdate;
        this.newFile = !new File(plugin.getDataFolder(), n).exists();
        loadConfig();
    }

    public void reloadConfig() {
        try {
            config.reload();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reload " + name, e);
        }
    }

    public YamlDocument getConfig() {
        return config;
    }

    public File getFile() {
        return config.getFile();
    }

    public boolean isNewFile() {
        return newFile;
    }

    public void saveConfig() {
        try {
            config.save();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save " + name, e);
        }
    }

    private void loadConfig() {
        try {
            config = ((BeastLibAPI) pl).getYamlFiles().load(
                    pl,
                    YamlFileOptions.builder(name)
                            .setResourcePath(name)
                            .setAutoUpdate(autoUpdate)
                            .setCreateFileIfMissing(true)
                            .setRequireDefaultResource(true)
                            .build()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + name, e);
        }
    }
}
