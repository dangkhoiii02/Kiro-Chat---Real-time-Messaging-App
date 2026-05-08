package com.ptithcm.kiro_mobile.data.model.user;

import com.google.gson.annotations.SerializedName;

public class PresenceUpdate {
    @SerializedName("userId")
    private String userId;

    @SerializedName("status")
    private String status; // "ONLINE" or "OFFLINE"

    @SerializedName("lastSeen")
    private String lastSeen;

    public PresenceUpdate() {}

    public String getUserId()   { return userId; }
    public String getStatus()   { return status; }
    public String getLastSeen() { return lastSeen; }
}
