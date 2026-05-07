package com.ptithcm.kiro_mobile.data.model.user;

import com.google.gson.annotations.SerializedName;

public class PublicProfile {
    @SerializedName("userId")
    private String userId;

    @SerializedName("username")
    private String username;

    @SerializedName("fullname")
    private String fullname;

    @SerializedName("emailAddress")
    private String emailAddress;

    @SerializedName("profilePictureUrl")
    private String profilePictureUrl;

    @SerializedName("connectionStatus")
    private String connectionStatus;

    @SerializedName("friendshipStatus")
    private String friendshipStatus;

    // Setters for manual construction (e.g. mapping ContactRequest)
    public void setUserId(String v)            { this.userId = v; }
    public void setUsername(String v)          { this.username = v; }
    public void setFullname(String v)          { this.fullname = v; }
    public void setProfilePictureUrl(String v) { this.profilePictureUrl = v; }
    public void setConnectionStatus(String v)  { this.connectionStatus = v; }

    public String getUserId()            { return userId; }
    public String getUsername()          { return username; }
    public String getFullname()          { return fullname; }
    public String getEmailAddress()      { return emailAddress; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public String getConnectionStatus()  { return connectionStatus; }
    public String getFriendshipStatus()  { return friendshipStatus; }

    public String getDisplayName() {
        if (fullname != null && !fullname.isEmpty()) return fullname;
        return username != null ? username : (emailAddress != null ? emailAddress : "");
    }
}
