package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a connected chat user.
 *
 * Immutable identity (username, joinTime); mutable online status only.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String        username;
    private final LocalDateTime joinTime;
    private volatile boolean    online;

    public User(String username) {
        this.username = username;
        this.joinTime = LocalDateTime.now();
        this.online   = true;
    }

    // ── Accessors ────────────────────────────────────────────────────────────
    public String        getUsername() { return username; }
    public LocalDateTime getJoinTime() { return joinTime; }
    public boolean       isOnline()    { return online; }
    public void          setOnline(boolean online) { this.online = online; }

    public String getJoinTimeFormatted() {
        return joinTime.format(FMT);
    }

    // ── Object identity is based on username alone ───────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return username.equals(u.username);
    }

    @Override
    public int hashCode() { return username.hashCode(); }

    @Override
    public String toString() { return username; }
}
