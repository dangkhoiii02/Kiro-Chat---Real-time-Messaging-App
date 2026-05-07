package com.ptithcm.kiro_mobile.data.model.chat;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.kiro_mobile.data.model.common.Page;

public class ConversationList {
    @SerializedName("conversations")
    private Page<Conversation> conversations;

    public Page<Conversation> getConversations() { return conversations; }
}
