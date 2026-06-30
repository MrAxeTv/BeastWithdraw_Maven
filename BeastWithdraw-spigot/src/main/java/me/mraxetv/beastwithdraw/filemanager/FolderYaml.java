package me.mraxetv.beastwithdraw.filemanager;

import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.api.yaml.YamlFileOptions;
import me.mraxetv.beastlib.lib.boostedyaml.YamlDocument;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class FolderYaml {
    private final JavaPlugin pl;
    private final String folder;
    private final String name;
    private final String resourceFolder;
    private final String resourceName;
    private final List<String> legacyRelativePaths;
    private final boolean newFile;
    private YamlDocument config;

    public FolderYaml(JavaPlugin plugin, String folder, String n) {
        this(plugin, folder, n, n);
    }

    public FolderYaml(JavaPlugin plugin, String folder, String n, String resourceName) {
        this(plugin, folder, n, folder, resourceName, null);
    }

    public FolderYaml(JavaPlugin plugin, String folder, String n, String resourceFolder, String resourceName, String... legacyRelativePaths) {
        this.pl = plugin;
        this.folder = normalizePath(folder);
        this.name = n;
        this.resourceFolder = normalizePath(resourceFolder == null || resourceFolder.trim().isEmpty() ? folder : resourceFolder);
        this.resourceName = resourceName == null || resourceName.trim().isEmpty() ? n : resourceName;
        this.legacyRelativePaths = normalizePaths(legacyRelativePaths);
        File targetFile = new File(plugin.getDataFolder(), this.folder + "/" + n);
        boolean legacyExists = hasLegacyFile();
        this.newFile = !targetFile.exists() && !legacyExists;
        migrateLegacyFile(targetFile);
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

    public boolean isNewFile() {
        return newFile;
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
        String resourcePath = resourceFolder + "/" + resourceName;
        try {
            config = ((BeastLibAPI) pl).getYamlFiles().load(
                    pl,
                    YamlFileOptions.builder(relativePath)
                            .setResourcePath(resourcePath)
                            .setAutoUpdate(true)
                            .setCreateFileIfMissing(true)
                            .setRequireDefaultResource(true)
                            .build()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + relativePath, e);
        }
    }

    private void migrateLegacyFile(File targetFile) {
        if (targetFile == null || targetFile.exists()) {
            return;
        }

        String legacyRelativePath = getFirstExistingLegacyPath();
        if (legacyRelativePath == null) {
            return;
        }

        File legacyFile = new File(pl.getDataFolder(), legacyRelativePath);
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create folder " + parent.getPath());
        }

        try {
            Files.move(legacyFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            pl.getLogger().info("Migrated " + legacyRelativePath + " to " + folder + "/" + name);
        } catch (IOException moveException) {
            try {
                Files.copy(legacyFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                pl.getLogger().info("Copied " + legacyRelativePath + " to " + folder + "/" + name);
            } catch (IOException copyException) {
                throw new IllegalStateException("Failed to migrate " + legacyRelativePath + " to " + folder + "/" + name, copyException);
            }
        }
    }

    private boolean hasLegacyFile() {
        return getFirstExistingLegacyPath() != null;
    }

    private String getFirstExistingLegacyPath() {
        for (String legacyRelativePath : legacyRelativePaths) {
            File legacyFile = new File(pl.getDataFolder(), legacyRelativePath);
            if (legacyFile.exists()) {
                return legacyRelativePath;
            }
        }
        return null;
    }

    private List<String> normalizePaths(String... values) {
        List<String> paths = new ArrayList<>();
        if (values == null) {
            return paths;
        }

        for (String value : values) {
            String normalized = normalizePath(value);
            if (normalized != null) {
                paths.add(normalized);
            }
        }
        return paths;
    }

    private String normalizePath(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? null : normalized;
    }
}
