package me.mraxetv.beastwithdraw.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CurrencyLogger {
    private static final Logger logger = Logger.getLogger("BeastCurrencyLogger");
    private static String LOG_DIRECTORY;

    private static final int MAX_LOG_FILE_SIZE = 10_000_000; // 10 MB
    private static final int MAX_NUM_LOG_FILES = 1000;
    private static final Level LOG_LEVEL = Level.ALL;

    private static FileHandler fileHandler;
    private static JavaPlugin pl;

    public CurrencyLogger(JavaPlugin pl) {
        this.pl = pl;
        LOG_DIRECTORY = pl.getDataFolder() + "/logs/";
        initLogger();
    }

    private void initLogger() {
        try {
            // Create logs directory if it doesn't exist
            File logsDir = new File(LOG_DIRECTORY);
            if (!logsDir.exists()) {
                logsDir.mkdir();
            }

            String logFileName = "latest.log";
            File logFile = new File(LOG_DIRECTORY + logFileName);
            if (logFile != null && logFile.exists()) compressLogFile(logFile);



            // Create the new log file if it doesn't exist
            //if (!logFile.exists()) logFile.createNewFile();



            // Configure the file handler with the new log file
            fileHandler = new FileHandler(logFile.getAbsolutePath(), false);
            fileHandler.setLevel(LOG_LEVEL);
            fileHandler.setFormatter(new CustomFormatter());

          // Add the file handler to the logger and set the logger level
            logger.addHandler(fileHandler);
            logger.setLevel(LOG_LEVEL);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize logger: " + e.getMessage());
        }

    }

    public static void logDeposit(String username, UUID uuid, String currency, double amount, int numNotes, double x, double y, double z) {
        logger.info(String.format("[%s] %s UUID:(%s) Deposit: %s +%s x%s at Location:(X: %s, Y: %s, Z: %s)", pl.getName(),username, uuid.toString(),
                currency, Utils.formatDouble(amount), numNotes, Utils.formatDouble(x), Utils.formatDouble(y), Utils.formatDouble(z)));
    }

    public static void logWithdraw(String username, UUID uuid, String currency, double amount, int numNotes, double x, double y, double z) {
        logger.info(String.format("[%s] User: %s UUID:(%s) Withdraw: %s -%s x%s at Location:(X: %s, Y: %s, Z: %s)", pl.getName(),username, uuid.toString(),
                currency, Utils.formatDouble(amount), numNotes, Utils.formatDouble(x), Utils.formatDouble(y), Utils.formatDouble(z)));
    }
    public static void logAdminNote(String admin, String username, UUID uuid, String currency, double amount, int numNotes, double x, double y, double z) {
        logger.info(String.format("[%s] Admin: %s UUID: (%s) Item: %s %s x%s at Location:(X: %s, Y: %s, Z: %s)", pl.getName(),username, uuid.toString(),
                currency, Utils.formatDouble(amount), numNotes, Utils.formatDouble(x), Utils.formatDouble(y), Utils.formatDouble(z)));
    }

    public static void close() {
        if (fileHandler != null) {
            logger.removeHandler(fileHandler);
            fileHandler.close();
        }
    }

    private void compressLogFile(File log) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String zipPrefix = dateFormat.format(new Date());
            String zipSuffix = ".zip";
            int latestZipFileIndex = 0;
            File[] existingLogFiles = new File(LOG_DIRECTORY).listFiles((dir, name) -> name.startsWith(zipPrefix) && name.endsWith(zipSuffix));
            if (existingLogFiles != null) {
                for (File logFile : existingLogFiles) {
                    String fileName = logFile.getName();
                    int startIndex = zipPrefix.length();
                    int endIndex = fileName.length() - zipSuffix.length();
                    int fileIndex = Math.abs(Integer.parseInt(fileName.substring(startIndex, endIndex)));
                    if (fileIndex > latestZipFileIndex) {
                        latestZipFileIndex = fileIndex;
                    }
                }
            }
            // Create a zip file with the current date and time in the filename

            String zipFileName = zipPrefix + "-" + (latestZipFileIndex+1)+zipSuffix;
            File zipFile = new File(LOG_DIRECTORY + zipFileName);
            zipFile.createNewFile();

            // Write the old log file to the zip file
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry ze = new ZipEntry(zipPrefix+".log");
            zos.putNextEntry(ze);
            FileInputStream fis = new FileInputStream(log);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            fis.close();
            zos.closeEntry();
            zos.close();

            close();

            // Log the compression event
            logger.log(Level.INFO, "["+pl.getName()+"] Log file compressed to: " + zipFile.getName());
            // Delete the old log file
            File[] allLogFiles = new File(LOG_DIRECTORY).listFiles((dir, name) -> name.startsWith("latest"));
            for(File f : allLogFiles) {
                f.delete();

            }




        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to compress log file: " + e.getMessage());
        }
    }
}

class CustomFormatter extends SimpleFormatter{

    @Override
    public String format(LogRecord record) {
        return record.getMessage() + System.lineSeparator();



    }
}



