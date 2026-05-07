package com.ptithcm.kiro_mobile.data.model.contact;

import com.google.gson.annotations.SerializedName;

public class ContactRequest {
    @SerializedName("requestId")
    private String requestId;

    @SerializedName("senderId")
    private String senderId;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("senderUsername")
    private String senderUsername;

    @SerializedName("senderAvatarUrl")
    private String senderAvatarUrl;

    @SerializedName("createdAt")
    private String createdAt;

    public String getRequestId()      { return requestId; }
    public String getSenderId()       { return senderId; }
    public String getSenderName()     { return senderName; }
    public String getSenderUsername() { return senderUsername; }
    public String getSenderAvatarUrl(){ return senderAvatarUrl; }
    public String getCreatedAt()      { return createdAt; }
}
