package me.mraxetv.beastwithdraw.filemanager;

import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.api.yaml.YamlFileOptions;
import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class FolderYaml {
    private final JavaPlugin pl;
    private final String folder;
    private final String name;
    private YamlDocument config;

    public FolderYaml(JavaPlugin plugin, String folder, String n) {
        this.pl = plugin;
        this.folder = folder;
        this.name = n;
        loadConfig();
    }

    public void reloadConfig() {
        try {
            config.reload();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reload " + folder + "/" + name, e);
        }
    }

    public YamlDocument getConfig() {
        return config;
    }

    public File getFile() {
        return config.getFile();
    }

    public void saveConfig() {
        try {
            config.save();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save " + folder + "/" + name, e);
        }
    }

    private void loadConfig() {
        String relativePath = folder + "/" + name;
        try {
            config = ((BeastLibAPI) pl).getYamlFiles().load(
                    pl,
                    YamlFileOptions.builder(relativePath)
                            .setResourcePath(relativePath)
                            .setAutoUpdate(true)
                            .setCreateFileIfMissing(true)
                            .setRequireDefaultResource(true)
                            .build()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + relativePath, e);
        }
    }
}
