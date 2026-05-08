package com.ptithcm.kiro_mobile.data.api;

import com.ptithcm.kiro_mobile.data.model.contact.BlockedUserList;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit interface for block/unblock user endpoints.
 */
public interface BlockApi {

    @GET("blocks")
    Call<BlockedUserList> getBlockedUsers();

    @POST("blocks/{blockUserId}")
    Call<Void> blockUser(@Path("blockUserId") String blockUserId);

    @DELETE("blocks/{blockUserId}")
    Call<Void> unblockUser(@Path("blockUserId") String blockUserId);
}
