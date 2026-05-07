package com.ptithcm.kiro_mobile.data.model.chat;

import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    @SerializedName("messageId")
    private String messageId;

    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("senderId")
    private String senderId;

    @SerializedName("type")
    private String type;

    @SerializedName("content")
    private String content;

    @SerializedName("mediaUrl")
    private String mediaUrl;

    @SerializedName("mediaName")
    private String mediaName;

    @SerializedName("messageState")
    private String messageState;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("replyToMessageId")
    private String replyToMessageId;

    @SerializedName("isDeleted")
    private boolean deleted;

    // Set by client — true if sent by current user
    private transient boolean isMine;

    public String getMessageId()       { return messageId; }
    public String getConversationId()  { return conversationId; }
    public String getSenderId()        { return senderId; }
    public String getType()            { return type; }
    public String getContent()         { return content; }
    public String getMediaUrl()        { return mediaUrl; }
    public String getMediaName()       { return mediaName; }
    public String getMessageState()    { return messageState; }
    public String getTimestamp()       { return timestamp; }
    public String getReplyToMessageId(){ return replyToMessageId; }
    public boolean isDeleted()         { return deleted; }
    public boolean isMine()            { return isMine; }
    public void setMine(boolean mine)  { this.isMine = mine; }
}
