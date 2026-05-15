package me.mraxetv.beastwithdraw.logging;

import me.mraxetv.beastwithdraw.BeastWithdrawPlugin;
import me.mraxetv.beastwithdraw.managers.AssetHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class WithdrawLogger {
    private static final String COMBINED_LOG_FOLDER = "Combined";

    private final BeastWithdrawPlugin plugin;
    private final File baseDirectory;
    private final Map<String, BufferedWriter> writers = new HashMap<>();
    private BufferedWriter combinedWriter;
    private Boolean consoleLoggingOverride;
    private final DecimalFormat amountFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private final SimpleDateFormat lineTimestamp = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat archiveDate = new SimpleDateFormat("yyyy-MM-dd");

    public WithdrawLogger(BeastWithdrawPlugin plugin) {
        this.plugin = plugin;
        this.baseDirectory = new File(plugin.getDataFolder(), "Logs");
    }

    public synchronized void prepareAssetLogs(AssetHandler assetHandler) {
        if (!shouldLog(assetHandler)) {
            return;
        }

        try {
            if (shouldWriteCombinedFile()) {
                getOrCreateCombinedWriter();
            }
            if (shouldWriteSeparateFile(assetHandler)) {
                getOrCreateWriter(assetHandler);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to prepare transaction log for " + assetHandler.getConfigName() + ": " + exception.getMessage());
        }
    }

    public void logWithdraw(AssetHandler assetHandler, Player player, double amountPerItem, int stackSize, double totalAmount, double balanceAfter) {
        if (!shouldLog(assetHandler)) {
            return;
        }

        String message = buildMessage(
                assetHandler,
                player,
                "WITHDRAW",
                "amount=" + formatAmount(amountPerItem)
                        + " stack=" + stackSize
                        + " total=" + formatAmount(totalAmount)
                        + " balance_after=" + formatAmount(balanceAfter)
        );
        write(assetHandler, message);
    }

    public void logRedeem(AssetHandler assetHandler, Player player, double amountPerItem, int stackSize, double totalAmount, double totalTax, double finalAmount, double balanceAfter) {
        if (!shouldLog(assetHandler)) {
            return;
        }

        String message = buildMessage(
                assetHandler,
                player,
                "REDEEM",
                "amount=" + formatAmount(amountPerItem)
                        + " stack=" + stackSize
                        + " total=" + formatAmount(totalAmount)
                        + " tax=" + formatAmount(totalTax)
                        + " final=" + formatAmount(finalAmount)
                        + " balance_after=" + formatAmount(balanceAfter)
        );
        write(assetHandler, message);
    }

    public synchronized void shutdown() {
        for (BufferedWriter writer : writers.values()) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        writers.clear();
        if (combinedWriter != null) {
            try {
                combinedWriter.close();
            } catch (IOException ignored) {
            }
            combinedWriter = null;
        }
        consoleLoggingOverride = null;
    }

    public synchronized boolean isConsoleLoggingEnabled() {
        if (consoleLoggingOverride != null) {
            return consoleLoggingOverride;
        }
        return isConsoleLoggingEnabledInConfig();
    }

    public synchronized boolean isConsoleLoggingEnabledInConfig() {
        return plugin.getSettings().getBoolean("Settings.Logs.Console.Enabled", true);
    }

    public synchronized boolean hasConsoleLoggingOverride() {
        return consoleLoggingOverride != null;
    }

    public synchronized void setConsoleLoggingOverride(Boolean enabled) {
        consoleLoggingOverride = enabled;
    }

    private boolean shouldLog(AssetHandler assetHandler) {
        return assetHandler != null
                && plugin.getSettings().getBoolean("Settings.Logs.Enabled", false)
                && assetHandler.isTransactionLoggingEnabled();
    }

    private boolean shouldWriteSeparateFile(AssetHandler assetHandler) {
        return shouldLog(assetHandler) && assetHandler.isSeparateLogFileEnabled();
    }

    private boolean shouldWriteCombinedFile() {
        return plugin.getSettings().getBoolean("Settings.Logs.CombinedFile.Enabled", false);
    }

    private String buildMessage(AssetHandler assetHandler, Player player, String action, String details) {
        Location location = player.getLocation();
        String worldName = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        return "[WithdrawLog][" + assetHandler.getConfigName() + "] action=" + action
                + " player=" + player.getName()
                + " uuid=" + player.getUniqueId()
                + " " + details
                + " world=" + worldName
                + " x=" + location.getBlockX()
                + " y=" + location.getBlockY()
                + " z=" + location.getBlockZ();
    }

    private synchronized void write(AssetHandler assetHandler, String message) {
        if (isConsoleLoggingEnabled()) {
            plugin.getLogger().info(message);
        }

        try {
            String formattedLine = "[" + lineTimestamp.format(new Date()) + " INFO]: " + message;

            if (shouldWriteCombinedFile()) {
                BufferedWriter combined = getOrCreateCombinedWriter();
                combined.write(formattedLine);
                combined.newLine();
                combined.flush();
            }

            if (shouldWriteSeparateFile(assetHandler)) {
                BufferedWriter writer = getOrCreateWriter(assetHandler);
                writer.write(formattedLine);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to write transaction log for " + assetHandler.getConfigName() + ": " + exception.getMessage());
        }
    }

    private BufferedWriter getOrCreateCombinedWriter() throws IOException {
        if (combinedWriter != null) {
            return combinedWriter;
        }

        File combinedDirectory = new File(baseDirectory, COMBINED_LOG_FOLDER);
        if (!combinedDirectory.exists() && !combinedDirectory.mkdirs()) {
            throw new IOException("Could not create log directory " + combinedDirectory.getAbsolutePath());
        }

        rotateLatestLog(combinedDirectory);

        File latestFile = new File(combinedDirectory, "latest.log");
        combinedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(latestFile, true), StandardCharsets.UTF_8));
        return combinedWriter;
    }

    private BufferedWriter getOrCreateWriter(AssetHandler assetHandler) throws IOException {
        String key = assetHandler.getConfigName().toLowerCase(Locale.ENGLISH);
        BufferedWriter existingWriter = writers.get(key);
        if (existingWriter != null) {
            return existingWriter;
        }

        File typeDirectory = new File(baseDirectory, assetHandler.getConfigName());
        if (!typeDirectory.exists() && !typeDirectory.mkdirs()) {
            throw new IOException("Could not create log directory " + typeDirectory.getAbsolutePath());
        }

        rotateLatestLog(typeDirectory);

        File latestFile = new File(typeDirectory, "latest.log");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(latestFile, true), StandardCharsets.UTF_8));
        writers.put(key, writer);
        return writer;
    }

    private void rotateLatestLog(File typeDirectory) throws IOException {
        File latestFile = new File(typeDirectory, "latest.log");
        if (!latestFile.exists() || latestFile.length() <= 0L) {
            return;
        }

        String datePrefix = archiveDate.format(new Date(latestFile.lastModified()));
        int index = 1;
        File archiveFile;
        do {
            archiveFile = new File(typeDirectory, datePrefix + "-" + index + ".log.gz");
            index++;
        } while (archiveFile.exists());

        byte[] buffer = new byte[8192];
        try (InputStream inputStream = new FileInputStream(latestFile);
             GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(archiveFile))) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }

        if (!latestFile.delete()) {
            throw new IOException("Could not rotate " + latestFile.getAbsolutePath());
        }
    }

    private String formatAmount(double amount) {
        synchronized (amountFormat) {
            return amountFormat.format(amount);
        }
    }
}
