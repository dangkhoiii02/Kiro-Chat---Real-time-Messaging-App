package com.ptithcm.kiro_mobile.data.model.contact;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.kiro_mobile.data.model.common.Page;
import com.ptithcm.kiro_mobile.data.model.user.PublicProfile;

public class FriendList {
    @SerializedName("friends")
    private Page<PublicProfile> friends;

    public Page<PublicProfile> getFriends() { return friends; }
}
