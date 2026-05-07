package com.ptithcm.kiro_mobile.data.model.contact;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.kiro_mobile.data.model.common.Page;
import com.ptithcm.kiro_mobile.data.model.user.PublicProfile;
import java.util.List;

public class UserSearchList {
    @SerializedName("users")
    private Page<PublicProfile> users;

    // Some endpoints return a flat list
    @SerializedName("content")
    private List<PublicProfile> content;

    public Page<PublicProfile> getUsers() { return users; }
    public List<PublicProfile> getContent() { return content; }
}
