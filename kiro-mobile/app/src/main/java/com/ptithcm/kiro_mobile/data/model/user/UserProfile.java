package com.ptithcm.kiro_mobile.data.model.user;

import com.google.gson.annotations.SerializedName;

public class UserProfile {

    @SerializedName("userId")
    private String userId;

    @SerializedName("firstname")
    private String firstname;

    @SerializedName("lastname")
    private String lastname;

    @SerializedName("username")
    private String username;

    @SerializedName("emailAddress")
    private String emailAddress;

    @SerializedName("profilePictureUrl")
    private String profilePictureUrl;

    @SerializedName("activityStatus")
    private boolean activityStatus;

    public String getUserId()           { return userId; }
    public String getFirstname()        { return firstname; }
    public String getLastname()         { return lastname; }
    public String getUsername()         { return username; }
    public String getEmailAddress()     { return emailAddress; }
    public String getProfilePictureUrl(){ return profilePictureUrl; }
    public boolean isActivityStatus()   { return activityStatus; }

    /** Returns "Firstname Lastname" or username as fallback. */
    public String getDisplayName() {
        if (firstname != null && !firstname.isEmpty()) {
            String full = firstname;
            if (lastname != null && !lastname.isEmpty()) full += " " + lastname;
            return full;
        }
        return username != null ? username : emailAddress;
    }
}
