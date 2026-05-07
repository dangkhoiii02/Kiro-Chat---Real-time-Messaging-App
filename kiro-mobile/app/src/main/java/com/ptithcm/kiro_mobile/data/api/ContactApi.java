package com.ptithcm.kiro_mobile.data.api;

import com.ptithcm.kiro_mobile.data.model.contact.ContactRequestList;
import com.ptithcm.kiro_mobile.data.model.contact.FriendList;
import com.ptithcm.kiro_mobile.data.model.contact.FriendshipResponse;
import com.ptithcm.kiro_mobile.data.model.contact.UserSearchList;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ContactApi {

    // ── Friends ──────────────────────────────────────────────────────────────

    @GET("friends")
    Call<FriendList> searchFriends(@Query("q") String query);

    @DELETE("friends/{friendId}")
    Call<FriendshipResponse> removeFriend(@Path("friendId") String friendId);

    // ── Contact requests ─────────────────────────────────────────────────────

    @GET("contact-requests")
    Call<ContactRequestList> getPendingRequests();

    @POST("contact-requests/{userId}")
    Call<FriendshipResponse> sendFriendRequest(@Path("userId") String userId);

    @DELETE("contact-requests/{userId}")
    Call<FriendshipResponse> unsendFriendRequest(@Path("userId") String userId);

    @POST("contact-requests/user/{requestUserId}/accept")
    Call<FriendshipResponse> acceptRequestFromUser(@Path("requestUserId") String requestUserId);

    @POST("contact-requests/user/{requestUserId}/reject")
    Call<FriendshipResponse> rejectRequestFromUser(@Path("requestUserId") String requestUserId);

    // ── User search / discovery ───────────────────────────────────────────────

    @GET("users/search")
    Call<UserSearchList> searchUsers(@Query("q") String query);

    @GET("users/suggests")
    Call<UserSearchList> getSuggestions();
}
