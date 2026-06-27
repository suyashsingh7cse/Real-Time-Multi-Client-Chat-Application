package service;

import model.ChatRoom;
import util.LoggerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of chat rooms: create, join, leave, query.
 *
 * All state lives in a ConcurrentHashMap so reads scale without locking.
 * Room creation is synchronized to eliminate TOCTOU gaps.
 */
public class RoomService {

    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();

    /**
     * Creates a room if one with that name does not already exist.
     *
     * @return true on success, false if name is taken.
     */
    public synchronized boolean createRoom(String roomName, String createdBy) {
        if (rooms.containsKey(roomName)) return false;
        rooms.put(roomName, new ChatRoom(roomName, createdBy));
        LoggerUtil.logServer("Room created: #" + roomName + " by " + createdBy);
        return true;
    }

    public boolean joinRoom(String roomName, String username) {
        ChatRoom room = rooms.get(roomName);
        if (room == null) return false;
        room.addMember(username);
        LoggerUtil.logServer(username + " joined #" + roomName);
        return true;
    }

    public boolean leaveRoom(String roomName, String username) {
        ChatRoom room = rooms.get(roomName);
        if (room == null || !room.hasMember(username)) return false;
        room.removeMember(username);
        LoggerUtil.logServer(username + " left #" + roomName);
        return true;
    }

    public boolean roomExists(String roomName) {
        return rooms.containsKey(roomName);
    }

    public ChatRoom getRoom(String roomName) {
        return rooms.get(roomName);
    }

    public Map<String, ChatRoom> getAllRooms() {
        return Collections.unmodifiableMap(rooms);
    }

    public int getRoomCount() {
        return rooms.size();
    }

    /** Returns all room names the given user is currently a member of. */
    public List<String> getRoomsForUser(String username) {
        List<String> result = new ArrayList<>();
        rooms.forEach((name, room) -> {
            if (room.hasMember(username)) result.add(name);
        });
        return result;
    }
}
