package service;

import model.User;
import util.LoggerUtil;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user registration and online presence.
 *
 * ConcurrentHashMap ensures thread-safe reads without a global lock.
 * The synchronized keyword on mutating methods prevents race conditions
 * during the check-then-act registration sequence.
 */
public class AuthenticationService {

    // username → User object
    private final Map<String, User> onlineUsers = new ConcurrentHashMap<>();

    /**
     * Attempts to register a new user.
     *
     * @return true if registration succeeded; false if username is already taken.
     */
    public synchronized boolean registerUser(User user) {
        if (onlineUsers.containsKey(user.getUsername())) {
            return false;
        }
        onlineUsers.put(user.getUsername(), user);
        LoggerUtil.logServer("Registered user: " + user.getUsername());
        return true;
    }

    /**
     * Removes a user and marks them offline.
     */
    public synchronized void removeUser(String username) {
        User removed = onlineUsers.remove(username);
        if (removed != null) {
            removed.setOnline(false);
            LoggerUtil.logServer("Removed user: " + username);
        }
    }

    public boolean isUsernameTaken(String username) {
        return onlineUsers.containsKey(username);
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public int getOnlineCount() {
        return onlineUsers.size();
    }

    /** Returns an unmodifiable snapshot of currently online users. */
    public Map<String, User> getOnlineUsers() {
        return Collections.unmodifiableMap(onlineUsers);
    }
}
