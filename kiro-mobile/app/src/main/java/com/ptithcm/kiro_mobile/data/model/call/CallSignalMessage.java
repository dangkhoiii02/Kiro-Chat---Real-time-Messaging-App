package com.ptithcm.kiro_mobile.data.model.call;

import com.google.gson.annotations.SerializedName;

/**
 * JSON object exchanged over STOMP for call signaling.
 * Types: OFFER, ANSWER, REJECT, END
 */
public class CallSignalMessage {

    @SerializedName("type")
    private String type;           // "OFFER" | "ANSWER" | "REJECT" | "END"

    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("targetUserId")
    private String targetUserId;

    @SerializedName("callerId")
    private String callerId;

    @SerializedName("callerName")
    private String callerName;

    @SerializedName("callerAvatar")
    private String callerAvatar;

    @SerializedName("withVideo")
    private boolean withVideo;

    // ── Constructors ─────────────────────────────────────────────────────────

    public CallSignalMessage() {}

    public CallSignalMessage(String type, String conversationId, String targetUserId,
                             String callerId, String callerName, String callerAvatar,
                             boolean withVideo) {
        this.type = type;
        this.conversationId = conversationId;
        this.targetUserId = targetUserId;
        this.callerId = callerId;
        this.callerName = callerName;
        this.callerAvatar = callerAvatar;
        this.withVideo = withVideo;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getType()             { return type; }
    public String getConversationId()   { return conversationId; }
    public String getTargetUserId()     { return targetUserId; }
    public String getCallerId()         { return callerId; }
    public String getCallerName()       { return callerName; }
    public String getCallerAvatar()     { return callerAvatar; }
    public boolean isWithVideo()        { return withVideo; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setType(String type)                     { this.type = type; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setTargetUserId(String targetUserId)     { this.targetUserId = targetUserId; }
    public void setCallerId(String callerId)             { this.callerId = callerId; }
    public void setCallerName(String callerName)         { this.callerName = callerName; }
    public void setCallerAvatar(String callerAvatar)     { this.callerAvatar = callerAvatar; }
    public void setWithVideo(boolean withVideo)          { this.withVideo = withVideo; }
}
