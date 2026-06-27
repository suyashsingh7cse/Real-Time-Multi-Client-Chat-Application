package service;

import model.Message;
import util.DateUtil;
import util.LoggerUtil;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Factory and counter for chat messages.
 *
 * Centralises message creation so that timestamping, history persistence,
 * and metric increment happen exactly once per message regardless of which
 * thread triggered the send.
 *
 * AtomicLong for messageCount is lock-free and correct under concurrent access.
 */
public class ChatService {

    private final HistoryService historyService;
    private final AtomicLong     messageCount = new AtomicLong(0);

    public ChatService(HistoryService historyService) {
        this.historyService = historyService;
    }

    // ── Message factories ──────────────────────────────────────────────────────

    public Message createBroadcastMessage(String sender, String content) {
        Message msg = build(sender, content, Message.Type.BROADCAST);
        historyService.saveGeneralMessage(msg);
        LoggerUtil.logChat("[BROADCAST] " + msg);
        return msg;
    }

    public Message createPrivateMessage(String sender, String recipient, String content) {
        Message msg = build(sender, content, Message.Type.PRIVATE);
        msg.setRecipient(recipient);
        historyService.savePrivateMessage(sender, recipient, msg);
        LoggerUtil.logChat("[PM] " + sender + " -> " + recipient + ": " + content);
        return msg;
    }

    public Message createRoomMessage(String sender, String roomName, String content) {
        Message msg = build(sender, content, Message.Type.ROOM);
        msg.setRecipient(roomName);
        historyService.saveRoomMessage(roomName, msg);
        LoggerUtil.logChat("[ROOM:#" + roomName + "] " + msg);
        return msg;
    }

    /** System messages are not persisted to history but are counted. */
    public Message createSystemMessage(String content) {
        return build("SERVER", content, Message.Type.SYSTEM);
    }

    // ── Metrics ───────────────────────────────────────────────────────────────

    public long getMessageCount() { return messageCount.get(); }

    // ── Private ───────────────────────────────────────────────────────────────

    private Message build(String sender, String content, Message.Type type) {
        messageCount.incrementAndGet();
        return new Message(sender, content, DateUtil.getCurrentTime(), type);
    }
}
