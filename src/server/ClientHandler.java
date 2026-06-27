package server;

import model.ChatRoom;
import model.Message;
import model.User;
import service.AuthenticationService;
import service.ChatService;
import service.HistoryService;
import service.RoomService;
import util.Constants;
import util.DateUtil;
import util.LoggerUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * Handles one connected client on a dedicated thread.
 *
 * Lifecycle:
 *   1. Authenticate (username negotiation)
 *   2. Main read loop – dispatch each line to broadcast / command handler
 *   3. Graceful teardown on /quit or connection loss
 *
 * Room messages are sent using the @roomName prefix convention:
 *   @general Hello everyone in general!
 */
public class ClientHandler implements Runnable {

    private final Socket                     socket;
    private final AuthenticationService      authService;
    private final ChatService                chatService;
    private final RoomService                roomService;
    private final HistoryService             historyService;
    private final Map<String, ClientHandler> handlers;   // shared registry

    private BufferedReader reader;
    private PrintWriter    writer;
    private User           user;
    private String         username;

    public ClientHandler(Socket socket,
                         AuthenticationService authService,
                         ChatService chatService,
                         RoomService roomService,
                         HistoryService historyService,
                         Map<String, ClientHandler> handlers) {
        this.socket         = socket;
        this.authService    = authService;
        this.chatService    = chatService;
        this.roomService    = roomService;
        this.historyService = historyService;
        this.handlers       = handlers;
    }

    // ── Runnable entry point ──────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            if (!authenticate()) {
                closeConnection();
                return;
            }

            broadcastSystem(username + " has joined the chat!");
            send(">> Welcome, " + username + "!  Type /help to see available commands.");
            send(">> " + authService.getOnlineCount() + " user(s) currently online.\n");

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("/")) {
                    if (line.equalsIgnoreCase(Constants.CMD_QUIT)) break;
                    handleCommand(line);
                } else if (line.startsWith("@")) {
                    handleRoomMessage(line);
                } else {
                    handleBroadcast(line);
                }
            }
        } catch (IOException e) {
            if (username != null) {
                LoggerUtil.logError("I/O error for " + username + ": " + e.getMessage());
            }
        } finally {
            teardown();
        }
    }

    // ── Authentication ────────────────────────────────────────────────────────

    private boolean authenticate() throws IOException {
        send("================================");
        send("  Real-Time Chat Application  ");
        send("      Powered by Java Sockets  ");
        send("================================");
        send("Enter your username:");

        for (int attempt = 0; attempt < Constants.MAX_AUTH_TRIES; attempt++) {
            String name = reader.readLine();
            if (name == null) return false;
            name = name.trim();

            String error = validateUsername(name);
            if (error != null) {
                send(">> " + error);
                send("   Try again (" + (Constants.MAX_AUTH_TRIES - attempt - 1) + " attempts left):");
                continue;
            }

            this.username = name;
            this.user     = new User(name);

            if (!authService.registerUser(user)) {
                send(">> Username '" + name + "' is already taken. Choose another:");
                this.username = null;
                this.user     = null;
                continue;
            }

            handlers.put(username, this);
            LoggerUtil.logServer("Authenticated: " + username
                    + " from " + socket.getInetAddress().getHostAddress());
            return true;
        }

        send(">> Too many failed attempts. Disconnecting.");
        return false;
    }

    private String validateUsername(String name) {
        if (name.isEmpty())
            return "Username cannot be empty.";
        if (name.length() < Constants.MIN_USERNAME_LENGTH || name.length() > Constants.MAX_USERNAME_LENGTH)
            return "Username must be " + Constants.MIN_USERNAME_LENGTH
                    + "-" + Constants.MAX_USERNAME_LENGTH + " characters.";
        if (!name.matches(Constants.USERNAME_REGEX))
            return "Username may only contain letters, digits, and underscores.";
        return null;
    }

    // ── Message routing ───────────────────────────────────────────────────────

    /** Broadcast a plain message to all connected clients. */
    private void handleBroadcast(String content) {
        Message msg = chatService.createBroadcastMessage(username, content);
        String  fmt = msg.toString();
        handlers.values().forEach(h -> h.send(fmt));
    }

    /**
     * Parses @roomName <message> and delivers to room members only.
     * If the room prefix is missing a space or message body, report usage.
     */
    private void handleRoomMessage(String line) {
        int spaceIdx = line.indexOf(' ');
        if (spaceIdx < 2) {
            send(">> Usage: @<roomName> <message>   e.g.  @general Hello!");
            return;
        }

        String roomName = line.substring(1, spaceIdx);
        String content  = line.substring(spaceIdx + 1).trim();

        if (!roomService.roomExists(roomName)) {
            send(">> Room '#" + roomName + "' does not exist. Use /create to make one.");
            return;
        }
        ChatRoom room = roomService.getRoom(roomName);
        if (!room.hasMember(username)) {
            send(">> You are not in '#" + roomName + "'. Use /join " + roomName + " first.");
            return;
        }
        if (content.isEmpty()) {
            send(">> Message body cannot be empty.");
            return;
        }

        Message msg = chatService.createRoomMessage(username, roomName, content);
        String  fmt = "[" + msg.getTimestamp() + "] [#" + roomName + "] " + username + ": " + content;
        deliverToRoom(room, fmt);
    }

    // ── Command dispatcher ────────────────────────────────────────────────────

    private void handleCommand(String line) {
        String[] parts   = line.split("\\s+", 3);
        String   command = parts[0].toLowerCase();

        switch (command) {
            case Constants.CMD_HELP    -> showHelp();
            case Constants.CMD_USERS   -> showUsers();
            case Constants.CMD_ROOMS   -> showRooms();
            case Constants.CMD_MSG     -> doPrivateMessage(parts);
            case Constants.CMD_CREATE  -> doCreateRoom(parts);
            case Constants.CMD_JOIN    -> doJoinRoom(parts);
            case Constants.CMD_LEAVE   -> doLeaveRoom(parts);
            case Constants.CMD_HISTORY -> doHistory(parts);
            default -> send(">> Unknown command '" + command + "'. Type /help for options.");
        }
    }

    // ── Command implementations ───────────────────────────────────────────────

    private void doPrivateMessage(String[] parts) {
        if (parts.length < 3) { send(">> Usage: /msg <username> <message>"); return; }

        String recipient = parts[1];
        String content   = parts[2];

        if (recipient.equalsIgnoreCase(username)) {
            send(">> You cannot send a private message to yourself.");
            return;
        }
        if (!authService.isUserOnline(recipient)) {
            send(">> '" + recipient + "' is not online.");
            return;
        }

        Message msg       = chatService.createPrivateMessage(username, recipient, content);
        String  senderFmt = "[" + msg.getTimestamp() + "] [PM → " + recipient + "] " + content;
        String  recvFmt   = "[" + msg.getTimestamp() + "] [PM ← " + username   + "] " + content;

        send(senderFmt);
        ClientHandler target = handlers.get(recipient);
        if (target != null) target.send(recvFmt);
    }

    private void doCreateRoom(String[] parts) {
        if (parts.length < 2) { send(">> Usage: /create <roomName>"); return; }

        String roomName = parts[1];
        if (!roomName.matches(Constants.ROOM_NAME_REGEX)) {
            send(">> Room name may only contain letters, digits, hyphens, and underscores.");
            return;
        }
        if (!roomService.createRoom(roomName, username)) {
            send(">> Room '#" + roomName + "' already exists. Use /join " + roomName + " to join it.");
            return;
        }
        roomService.joinRoom(roomName, username);
        send(">> Room '#" + roomName + "' created and joined.");
        send(">> Send messages to this room with:  @" + roomName + " Hello!");
    }

    private void doJoinRoom(String[] parts) {
        if (parts.length < 2) { send(">> Usage: /join <roomName>"); return; }

        String roomName = parts[1];
        if (!roomService.roomExists(roomName)) {
            send(">> Room '#" + roomName + "' does not exist. Use /create " + roomName + ".");
            return;
        }
        ChatRoom room = roomService.getRoom(roomName);
        if (room.hasMember(username)) {
            send(">> You are already in '#" + roomName + "'.");
            return;
        }
        roomService.joinRoom(roomName, username);
        send(">> Joined '#" + roomName + "' (" + room.getMemberCount() + " member(s)).");
        send(">> Send messages here with:  @" + roomName + " Hello!");

        // Show recent room history
        List<String> history = historyService.loadRoomHistory(roomName, 10);
        if (!history.isEmpty()) {
            send("── Last " + history.size() + " messages in #" + roomName + " ──");
            history.forEach(this::send);
            send("── End of history ──");
        }

        broadcastToRoom(room, "SERVER", username + " joined the room.");
    }

    private void doLeaveRoom(String[] parts) {
        if (parts.length < 2) { send(">> Usage: /leave <roomName>"); return; }

        String roomName = parts[1];
        if (!roomService.roomExists(roomName)) {
            send(">> Room '#" + roomName + "' does not exist.");
            return;
        }
        ChatRoom room = roomService.getRoom(roomName);
        if (!room.hasMember(username)) {
            send(">> You are not in '#" + roomName + "'.");
            return;
        }
        roomService.leaveRoom(roomName, username);
        broadcastToRoom(room, "SERVER", username + " left the room.");
        send(">> You left '#" + roomName + "'.");
    }

    private void doHistory(String[] parts) {
        if (parts.length < 2) {
            // General history
            List<String> history = historyService.loadGeneralHistory(Constants.HISTORY_LIMIT);
            send("── General Chat History (last " + history.size() + " messages) ──");
            if (history.isEmpty()) send("   (no messages yet)");
            else history.forEach(this::send);
            send("── End ──");
            return;
        }

        String target = parts[1];
        if (roomService.roomExists(target)) {
            List<String> h = historyService.loadRoomHistory(target, Constants.HISTORY_LIMIT);
            send("── #" + target + " History ──");
            if (h.isEmpty()) send("   (no messages yet)");
            else h.forEach(this::send);
            send("── End ──");
        } else if (authService.isUserOnline(target)) {
            List<String> h = historyService.loadPrivateHistory(username, target, Constants.HISTORY_LIMIT);
            send("── Private Messages with " + target + " ──");
            if (h.isEmpty()) send("   (no messages yet)");
            else h.forEach(this::send);
            send("── End ──");
        } else {
            send(">> No room or online user found with name '" + target + "'.");
        }
    }

    // ── Info commands ─────────────────────────────────────────────────────────

    private void showHelp() {
        send("================================");
        send("      AVAILABLE COMMANDS        ");
        send("================================");
        send("  /help               Show this help");
        send("  /users              List online users");
        send("  /rooms              List active rooms");
        send("  /msg <user> <msg>   Private message");
        send("  /create <room>      Create a chat room");
        send("  /join <room>        Join a chat room");
        send("  /leave <room>       Leave a chat room");
        send("  /history            General chat history");
        send("  /history <room>     Room history");
        send("  /history <user>     Private message history");
        send("  /quit               Disconnect");
        send("================================");
        send("  Room message syntax:");
        send("  @<roomName> <message>");
        send("  e.g.  @devs Fixed the null pointer!");
        send("================================");
    }

    private void showUsers() {
        Map<String, ?> all = authService.getOnlineUsers();
        send("================================");
        send("  Online Users (" + all.size() + ")");
        send("================================");
        all.keySet().forEach(u ->
                send("  " + u + (u.equals(username) ? "  ← you" : "")));
        send("================================");
    }

    private void showRooms() {
        Map<String, ChatRoom> all = roomService.getAllRooms();
        send("================================");
        send("  Active Rooms (" + all.size() + ")");
        send("================================");
        if (all.isEmpty()) {
            send("  No rooms yet. Create one with /create <name>");
        } else {
            all.forEach((name, room) -> {
                boolean member = room.hasMember(username);
                send("  #" + name
                        + "  [" + room.getMemberCount() + " member(s)]"
                        + (member ? "  ← joined" : ""));
            });
        }
        send("================================");
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────

    /** Deliver a formatted string to every member currently in the room. */
    private void deliverToRoom(ChatRoom room, String formatted) {
        room.getMembers().forEach(member -> {
            ClientHandler h = handlers.get(member);
            if (h != null) h.send(formatted);
        });
    }

    /** Create a system notification and send it to every member of a room. */
    private void broadcastToRoom(ChatRoom room, String sender, String content) {
        String ts  = DateUtil.getCurrentTime();
        String fmt = "[" + ts + "] [#" + room.getName() + "] " + sender + ": " + content;
        deliverToRoom(room, fmt);
    }

    /** Create a SERVER message and push it to every connected client. */
    private void broadcastSystem(String content) {
        Message msg = chatService.createSystemMessage(content);
        String  fmt = "[" + msg.getTimestamp() + "] [SERVER]: " + content;
        handlers.values().forEach(h -> h.send(fmt));
        LoggerUtil.logServer(content);
    }

    // ── Outbound write ────────────────────────────────────────────────────────

    public void send(String message) {
        if (writer != null && !socket.isClosed()) {
            writer.println(message);
        }
    }

    public String getUsername() { return username; }

    // ── Teardown ──────────────────────────────────────────────────────────────

    private void teardown() {
        if (username != null) {
            handlers.remove(username);
            authService.removeUser(username);

            // Silently leave every room this user was in
            roomService.getRoomsForUser(username).forEach(roomName -> {
                ChatRoom room = roomService.getRoom(roomName);
                roomService.leaveRoom(roomName, username);
                if (room != null) broadcastToRoom(room, "SERVER", username + " left the room.");
            });

            broadcastSystem(username + " has left the chat.");
            LoggerUtil.logServer("Disconnected: " + username);
        }
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException e) {
            LoggerUtil.logError("Error closing socket: " + e.getMessage());
        }
    }
}
