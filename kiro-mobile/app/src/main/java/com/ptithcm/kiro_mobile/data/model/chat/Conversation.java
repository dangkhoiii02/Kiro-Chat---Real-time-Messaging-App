package com.ptithcm.kiro_mobile.data.model.chat;

import com.google.gson.annotations.SerializedName;

/**
 * Matches the flat RestChat payload from the backend:
 * conversationId, conversationName, avatarUrl, isGroup,
 * unreadCount, lastMessage, lastMessageContent, isOnline, etc.
 */
public class Conversation {

    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("conversationName")
    private String conversationName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("isGroup")
    private boolean isGroup;

    @SerializedName("unreadCount")
    private long unreadCount;

    @SerializedName("lastMessage")
    private ChatMessage lastMessage;

    @SerializedName("lastMessageContent")
    private String lastMessageContent;

    @SerializedName("isOnline")
    private boolean isOnline;

    @SerializedName("isArchived")
    private boolean isArchived;

    @SerializedName("followingUp")
    private boolean followingUp;

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getConversationId()   { return conversationId; }
    public String getConversationName() { return conversationName; }
    public String getAvatarUrl()        { return avatarUrl; }
    public boolean isGroup()            { return isGroup; }
    public long getUnreadCount()        { return unreadCount; }
    public ChatMessage getLastMessage() { return lastMessage; }
    public String getLastMessageContent(){ return lastMessageContent; }
    public boolean isOnline()           { return isOnline; }

    // ── Display helpers ───────────────────────────────────────────────────────

    public String getDisplayName() {
        if (conversationName != null && !conversationName.isEmpty()) return conversationName;
        return "Cuộc trò chuyện";
    }

    public String getLastMessagePreview() {
        // Prefer the dedicated preview field
        if (lastMessageContent != null && !lastMessageContent.isEmpty()) return lastMessageContent;
        if (lastMessage == null) return "";
        String type = lastMessage.getType();
        if (type == null) return lastMessage.getContent() != null ? lastMessage.getContent() : "";
        switch (type.toLowerCase()) {
            case "image": return "[Hình ảnh]";
            case "file":  return "[Tệp đính kèm]";
            case "call":  return "[Cuộc gọi]";
            default:
                return lastMessage.getContent() != null ? lastMessage.getContent() : "";
        }
    }
}
