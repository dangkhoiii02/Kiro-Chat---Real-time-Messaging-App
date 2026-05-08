package com.ptithcm.kiro_mobile.data.model.call;

import com.google.gson.annotations.SerializedName;

/**
 * Response from GET /calls/{conversationId}/token
 */
public class CallTokenResponse {

    @SerializedName("token")
    private String token;

    public CallTokenResponse() {}

    public String getToken() { return token; }
}
