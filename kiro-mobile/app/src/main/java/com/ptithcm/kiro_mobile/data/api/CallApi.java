package com.ptithcm.kiro_mobile.data.api;

import com.ptithcm.kiro_mobile.data.model.call.CallTokenResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for call-related endpoints.
 */
public interface CallApi {

    @GET("calls/{conversationId}/token")
    Call<CallTokenResponse> getToken(
        @Path("conversationId") String conversationId,
        @Query("platform") String platform  // "android"
    );
}
