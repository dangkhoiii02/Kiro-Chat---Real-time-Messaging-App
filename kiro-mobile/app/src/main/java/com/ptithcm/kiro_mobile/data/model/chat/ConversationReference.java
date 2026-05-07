package com.ptithcm.kiro_mobile.data.model.chat;

import com.google.gson.annotations.SerializedName;

public class ConversationReference {
    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("id")
    private String id;

    @SerializedName("conversationType")
    private String conversationType;

    @SerializedName("createdDate")
    private String createdDate;

    public String getConversationId()   { return conversationId != null ? conversationId : id; }
    public String getConversationType() { return conversationType; }
    public String getCreatedDate()      { return createdDate; }
}
