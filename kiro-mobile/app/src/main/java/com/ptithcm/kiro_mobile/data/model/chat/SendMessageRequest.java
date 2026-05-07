package com.ptithcm.kiro_mobile.data.model.chat;

import com.google.gson.annotations.SerializedName;

public class SendMessageRequest {
    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("content")
    private String content;

    @SerializedName("type")
    private String type; // text | image | file | video | audio

    @SerializedName("replyToMessageId")
    private String replyToMessageId;

    public SendMessageRequest(String conversationId, String content, String type) {
        this.conversationId = conversationId;
        this.content = content;
        this.type = type;
    }

    public String getConversationId()   { return conversationId; }
    public String getContent()          { return content; }
    public String getType()             { return type; }
    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String id) { this.replyToMessageId = id; }
}
