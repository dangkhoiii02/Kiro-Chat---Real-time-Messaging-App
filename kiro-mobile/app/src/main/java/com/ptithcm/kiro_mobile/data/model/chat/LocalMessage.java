package com.ptithcm.kiro_mobile.data.model.chat;

import java.util.UUID;

/**
 * Mobile-side message model that wraps the server ChatMessage and adds
 * local state for optimistic UI (LOCAL_PENDING, LOCAL_FAILED).
 */
public class LocalMessage {

    public enum Status {
        LOCAL_PENDING,   // sent to API, waiting for response
        LOCAL_FAILED,    // API call failed — show retry
        SENT,            // server confirmed, state = PREPARE/SENT
        DELIVERED,       // server state = DELIVERED
        SEEN             // server state = SEEN
    }

    // Stable local ID generated before the API call
    private final String localId;

    // Null until the server responds
    private String messageId;

    private String conversationId;
    private String senderId;
    private String content;
    private String type;       // text | image | file | call
    private String mediaUrl;
    private String mediaName;
    private String timestamp;
    private String replyToMessageId;
    private boolean deleted;
    private boolean isMine;
    private Status status;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Create a new outgoing message before sending. */
    public static LocalMessage outgoing(String conversationId, String content,
                                        String type, String currentUserId) {
        LocalMessage m = new LocalMessage(UUID.randomUUID().toString());
        m.conversationId = conversationId;
        m.content        = content;
        m.type           = type;
        m.senderId       = currentUserId;
        m.isMine         = true;
        m.status         = Status.LOCAL_PENDING;
        m.timestamp      = String.valueOf(System.currentTimeMillis());
        return m;
    }

    /** Wrap a server-returned ChatMessage. */
    public static LocalMessage fromServer(ChatMessage msg, String currentUserId) {
        LocalMessage m = new LocalMessage(UUID.randomUUID().toString());
        m.messageId      = msg.getMessageId();
        m.conversationId = msg.getConversationId();
        m.senderId       = msg.getSenderId();
        m.content        = msg.getContent();
        m.type           = msg.getType();
        m.mediaUrl       = msg.getMediaUrl();
        m.mediaName      = msg.getMediaName();
        m.timestamp      = msg.getTimestamp();
        m.replyToMessageId = msg.getReplyToMessageId();
        m.deleted        = msg.isDeleted();
        m.isMine         = currentUserId != null && currentUserId.equals(msg.getSenderId());
        m.status         = mapServerState(msg.getMessageState());
        return m;
    }

    private LocalMessage(String localId) {
        this.localId = localId;
    }

    // ── Mutators ─────────────────────────────────────────────────────────────

    /** Called when the server responds successfully to a send. */
    public void confirmSent(ChatMessage serverMsg) {
        this.messageId = serverMsg.getMessageId();
        this.timestamp = serverMsg.getTimestamp();
        this.status    = mapServerState(serverMsg.getMessageState());
    }

    public void markFailed()  { this.status = Status.LOCAL_FAILED; }
    public void markPending() { this.status = Status.LOCAL_PENDING; }

    public void updateStatus(String serverState) {
        this.status = mapServerState(serverState);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String  getLocalId()          { return localId; }
    public String  getMessageId()        { return messageId; }
    public String  getConversationId()   { return conversationId; }
    public String  getSenderId()         { return senderId; }
    public String  getContent()          { return content; }
    public String  getType()             { return type; }
    public String  getMediaUrl()         { return mediaUrl; }
    public String  getMediaName()        { return mediaName; }
    public String  getTimestamp()        { return timestamp; }
    public String  getReplyToMessageId() { return replyToMessageId; }
    public boolean isDeleted()           { return deleted; }
    public boolean isMine()              { return isMine; }
    public Status  getStatus()           { return status; }

    public boolean isPending() { return status == Status.LOCAL_PENDING; }
    public boolean isFailed()  { return status == Status.LOCAL_FAILED; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Status mapServerState(String state) {
        if (state == null) return Status.SENT;
        switch (state.toUpperCase()) {
            case "DELIVERED": return Status.DELIVERED;
            case "SEEN":      return Status.SEEN;
            default:          return Status.SENT;
        }
    }
}
