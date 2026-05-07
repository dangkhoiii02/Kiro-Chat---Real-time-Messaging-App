package com.ptithcm.kiro_mobile.data.model.chat;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateGroupRequest {
    @SerializedName("groupName")
    private final String groupName;

    @SerializedName("profileImage")
    private final String profileImage;

    @SerializedName("memberIds")
    private final List<String> memberIds;

    public CreateGroupRequest(String groupName, String profileImage, List<String> memberIds) {
        this.groupName = groupName;
        this.profileImage = profileImage;
        this.memberIds = memberIds;
    }
}
