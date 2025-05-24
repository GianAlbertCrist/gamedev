package com.budgetapp.thrifty.utils;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLogger {
    private static final String LOG_FILE_NAME = "thrifty_log.txt";

    public static void log(Context context, String tag, String message) {
        logToFile(context, "INFO", tag, message);
    }

    public static void logError(Context context, String tag, String message, Throwable throwable) {
        logToFile(context, "ERROR", tag, message + " | Exception: " + throwable);
    }

    private static void logToFile(Context context, String level, String tag, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = String.format("%s [%s] %s: %s\n", timestamp, level, tag, message);

        try {
            File logFile = new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
            FileWriter writer = new FileWriter(logFile, true);
            writer.append(logEntry);
            writer.close();
        } catch (IOException e) {
            android.util.Log.e("AppLogger", "Failed to write log to file", e);
        }
    }
}