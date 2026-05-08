package com.ptithcm.kiro_mobile.data.model.contact;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a user who has been blocked by the current user.
 */
public class BlockedUser {

    @SerializedName("userId")
    private String userId;

    @SerializedName("username")
    private String username;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("blockedAt")
    private String blockedAt;

    public BlockedUser() {}

    public String getUserId()    { return userId; }
    public String getUsername()  { return username; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBlockedAt() { return blockedAt; }
}
