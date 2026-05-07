package com.ptithcm.kiro_mobile.data.model.chat;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MessageList {
    @SerializedName("messages")
    private List<ChatMessage> messages;

    @SerializedName("hasMore")
    private boolean hasMore;

    @SerializedName("nextBeforeMessageId")
    private String nextBeforeMessageId;

    public List<ChatMessage> getMessages()       { return messages; }
    public boolean isHasMore()                   { return hasMore; }
    public String getNextBeforeMessageId()       { return nextBeforeMessageId; }
}
