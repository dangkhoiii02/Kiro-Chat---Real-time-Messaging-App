package com.ptithcm.kiro_mobile.data.model.contact;

import com.google.gson.annotations.SerializedName;

public class FriendshipResponse {
    @SerializedName("status")
    private String status;

    public String getStatus() { return status; }
}
