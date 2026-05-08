package com.ptithcm.kiro_mobile.data.model.group;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.kiro_mobile.data.model.user.UserProfile;

public class GroupParticipant {
    @SerializedName("role")
    private String role; // "OWNER" or "MEMBER"

    @SerializedName("participantUser")
    private UserProfile participantUser;

    public String getRole() { return role; }
    public UserProfile getParticipantUser() { return participantUser; }
}
