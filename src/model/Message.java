package model;

import java.io.Serializable;

/**
 * Immutable value object representing a single chat message.
 *
 * Type enum encodes the delivery channel so the rendering layer
 * can format or route without inspecting raw content.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        BROADCAST,       // visible to everyone
        PRIVATE,         // sender ↔ one recipient
        ROOM,            // members of a named room
        SYSTEM           // server-generated notification
    }

    private final String sender;
    private final String content;
    private final String timestamp;   // pre-formatted HH:mm:ss
    private final Type   type;
    private       String recipient;   // username (PRIVATE) or room name (ROOM)

    public Message(String sender, String content, String timestamp, Type type) {
        this.sender    = sender;
        this.content   = content;
        this.timestamp = timestamp;
        this.type      = type;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public String getSender()    { return sender; }
    public String getContent()   { return content; }
    public String getTimestamp() { return timestamp; }
    public Type   getType()      { return type; }
    public String getRecipient() { return recipient; }
    public void   setRecipient(String recipient) { this.recipient = recipient; }

    /** Human-readable form used when persisting to history files. */
    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}
