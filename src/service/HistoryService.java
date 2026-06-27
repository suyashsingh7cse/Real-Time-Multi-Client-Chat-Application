package service;

import model.Message;
import util.Constants;
import util.LoggerUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for persisting and retrieving chat history from the file system.
 *
 * Three distinct storage areas:
 *   data/history/general.txt        – broadcast messages
 *   data/history/pm_A_B.txt         – private conversation (alphabetical)
 *   data/rooms/<roomName>.txt        – per-room messages
 *
 * All write operations are synchronized on a single lock to avoid
 * interleaved writes from concurrent threads.
 */
public class HistoryService {

    private final Object writeLock = new Object();

    public HistoryService() {
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(Paths.get(Constants.HISTORY_DIR));
            Files.createDirectories(Paths.get(Constants.ROOMS_DIR));
            Files.createDirectories(Paths.get(Constants.USERS_DIR));
            Files.createDirectories(Paths.get(Constants.LOGS_DIR));
        } catch (IOException e) {
            LoggerUtil.logError("Failed to create data directories: " + e.getMessage());
        }
    }

    // ── Write operations ───────────────────────────────────────────────────────

    public void saveGeneralMessage(Message message) {
        append(Constants.GENERAL_HISTORY, message.toString());
    }

    public void savePrivateMessage(String sender, String recipient, Message message) {
        append(Constants.HISTORY_DIR + privateFilename(sender, recipient), message.toString());
    }

    public void saveRoomMessage(String roomName, Message message) {
        append(Constants.ROOMS_DIR + roomName + ".txt", message.toString());
    }

    // ── Read operations ────────────────────────────────────────────────────────

    public List<String> loadGeneralHistory(int limit) {
        return tail(Constants.GENERAL_HISTORY, limit);
    }

    public List<String> loadPrivateHistory(String user1, String user2, int limit) {
        return tail(Constants.HISTORY_DIR + privateFilename(user1, user2), limit);
    }

    public List<String> loadRoomHistory(String roomName, int limit) {
        return tail(Constants.ROOMS_DIR + roomName + ".txt", limit);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Derive a canonical, alphabetically-ordered filename for a private conversation. */
    private String privateFilename(String a, String b) {
        String[] pair = {a, b};
        Arrays.sort(pair);
        return "pm_" + pair[0] + "_" + pair[1] + ".txt";
    }

    private void append(String path, String line) {
        synchronized (writeLock) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(path, true))) {
                bw.write(line);
                bw.newLine();
            } catch (IOException e) {
                LoggerUtil.logError("Write failed [" + path + "]: " + e.getMessage());
            }
        }
    }

    /** Returns the last {@code limit} lines of a file (or fewer if the file is shorter). */
    private List<String> tail(String path, int limit) {
        List<String> lines = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) return lines;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            LoggerUtil.logError("Read failed [" + path + "]: " + e.getMessage());
        }

        int size = lines.size();
        return size > limit ? lines.subList(size - limit, size) : lines;
    }
}
