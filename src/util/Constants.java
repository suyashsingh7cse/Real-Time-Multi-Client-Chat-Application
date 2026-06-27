package util;

/**
 * Application-wide constants for configuration, file paths, and command tokens.
 */
public final class Constants {

    private Constants() {}

    // Network
    public static final int    DEFAULT_PORT    = 6000;
    public static final String SERVER_HOST     = "localhost";
    public static final int    MAX_AUTH_TRIES  = 3;

    // File paths
    public static final String DATA_DIR        = "data/";
    public static final String HISTORY_DIR     = "data/history/";
    public static final String ROOMS_DIR       = "data/rooms/";
    public static final String USERS_DIR       = "data/users/";
    public static final String LOGS_DIR        = "logs/";

    public static final String SERVER_LOG      = "logs/server.log";
    public static final String CHAT_LOG        = "logs/chat.log";
    public static final String ERROR_LOG       = "logs/errors.log";
    public static final String GENERAL_HISTORY = "data/history/general.txt";

    // History
    public static final int HISTORY_LIMIT = 20;

    // Commands
    public static final String CMD_HELP    = "/help";
    public static final String CMD_USERS   = "/users";
    public static final String CMD_MSG     = "/msg";
    public static final String CMD_CREATE  = "/create";
    public static final String CMD_JOIN    = "/join";
    public static final String CMD_LEAVE   = "/leave";
    public static final String CMD_ROOMS   = "/rooms";
    public static final String CMD_HISTORY = "/history";
    public static final String CMD_QUIT    = "/quit";

    // Validation
    public static final int    MIN_USERNAME_LENGTH = 2;
    public static final int    MAX_USERNAME_LENGTH = 20;
    public static final String USERNAME_REGEX      = "[a-zA-Z0-9_]+";
    public static final String ROOM_NAME_REGEX     = "[a-zA-Z0-9_-]+";

    // Dashboard refresh interval (ms)
    public static final long DASHBOARD_INTERVAL = 30_000L;
    public static final long DASHBOARD_DELAY    = 10_000L;
}
