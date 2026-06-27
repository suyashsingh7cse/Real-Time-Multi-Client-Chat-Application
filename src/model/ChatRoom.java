package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Represents a named chat room.
 *
 * Thread-safe: CopyOnWriteArraySet for membership (read-heavy);
 * synchronizedList for in-memory message cache.
 */
public class ChatRoom implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String       name;
    private final String       createdBy;
    private final Set<String>  members;
    private final List<Message> messageCache;

    public ChatRoom(String name, String createdBy) {
        this.name         = name;
        this.createdBy    = createdBy;
        this.members      = new CopyOnWriteArraySet<>();
        this.messageCache = Collections.synchronizedList(new ArrayList<>());
    }

    // ── Membership ─────────────────────────────────────────────────────────────
    public void    addMember(String username)    { members.add(username); }
    public void    removeMember(String username) { members.remove(username); }
    public boolean hasMember(String username)    { return members.contains(username); }
    public int     getMemberCount()              { return members.size(); }
    public Set<String> getMembers()              { return Collections.unmodifiableSet(members); }

    // ── In-memory cache (max 100 recent messages) ───────────────────────────
    public void addMessage(Message message) {
        messageCache.add(message);
        if (messageCache.size() > 100) {
            messageCache.remove(0);
        }
    }

    public List<Message> getMessageCache() {
        return Collections.unmodifiableList(messageCache);
    }

    // ── Accessors ──────────────────────────────────────────────────────────────
    public String getName()      { return name; }
    public String getCreatedBy() { return createdBy; }

    @Override
    public String toString() {
        return "#" + name + " [" + members.size() + " members, created by " + createdBy + "]";
    }
}
