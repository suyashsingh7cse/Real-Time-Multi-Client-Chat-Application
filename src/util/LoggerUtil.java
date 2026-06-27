package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Thread-safe logger that writes structured entries to dedicated log files.
 *
 * Three channels:
 *   server.log  – lifecycle events (connections, room changes)
 *   chat.log    – all messages (broadcast, private, room)
 *   errors.log  – exceptions and unexpected states
 */
public final class LoggerUtil {

    private static final Object FILE_LOCK = new Object();

    private LoggerUtil() {}

    public static void logServer(String message) {
        writeLog(Constants.SERVER_LOG, "[SERVER] " + DateUtil.getCurrentDateTime() + " | " + message);
    }

    public static void logChat(String message) {
        writeLog(Constants.CHAT_LOG, "[CHAT]   " + DateUtil.getCurrentDateTime() + " | " + message);
    }

    public static void logError(String message) {
        String entry = "[ERROR]  " + DateUtil.getCurrentDateTime() + " | " + message;
        writeLog(Constants.ERROR_LOG, entry);
        System.err.println(entry);
    }

    private static void writeLog(String filePath, String entry) {
        synchronized (FILE_LOCK) {
            try {
                Files.createDirectories(Paths.get(Constants.LOGS_DIR));
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                    writer.write(entry);
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("[LoggerUtil] Failed to write to " + filePath + ": " + e.getMessage());
            }
        }
    }
}
