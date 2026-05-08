package com.ptithcm.kiro_mobile.data.model.group;

import com.google.gson.annotations.SerializedName;

public class GroupProfile {
    @SerializedName("groupId")
    private String groupId;

    @SerializedName("groupName")
    private String groupName;

    @SerializedName("profileImage")
    private String profileImage;

    @SerializedName("membersCount")
    private int membersCount;

    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getProfileImage() { return profileImage; }
    public int getMembersCount() { return membersCount; }
}
