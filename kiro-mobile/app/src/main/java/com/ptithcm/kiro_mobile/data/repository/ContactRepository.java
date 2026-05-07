package com.ptithcm.kiro_mobile.data.repository;

import android.content.Context;

import com.ptithcm.kiro_mobile.data.api.ApiClient;
import com.ptithcm.kiro_mobile.data.model.contact.ContactRequestList;
import com.ptithcm.kiro_mobile.data.model.contact.FriendList;
import com.ptithcm.kiro_mobile.data.model.contact.FriendshipResponse;
import com.ptithcm.kiro_mobile.data.model.contact.UserSearchList;
import com.ptithcm.kiro_mobile.util.Result;

import java.io.IOException;

import retrofit2.Response;

public class ContactRepository {

    private final ApiClient apiClient;

    public ContactRepository(Context context) {
        this.apiClient = ApiClient.getInstance(context);
    }

    public Result<FriendList> searchFriends(String query) {
        return execute(apiClient.getContactApi().searchFriends(query));
    }

    public Result<FriendshipResponse> sendFriendRequest(String userId) {
        return execute(apiClient.getContactApi().sendFriendRequest(userId));
    }

    public Result<FriendshipResponse> unsendFriendRequest(String userId) {
        return execute(apiClient.getContactApi().unsendFriendRequest(userId));
    }

    public Result<FriendshipResponse> acceptRequest(String requestUserId) {
        return execute(apiClient.getContactApi().acceptRequestFromUser(requestUserId));
    }

    public Result<FriendshipResponse> rejectRequest(String requestUserId) {
        return execute(apiClient.getContactApi().rejectRequestFromUser(requestUserId));
    }

    public Result<FriendshipResponse> removeFriend(String friendId) {
        return execute(apiClient.getContactApi().removeFriend(friendId));
    }

    public Result<ContactRequestList> getPendingRequests() {
        return execute(apiClient.getContactApi().getPendingRequests());
    }

    public Result<UserSearchList> searchUsers(String query) {
        return execute(apiClient.getContactApi().searchUsers(query));
    }

    public Result<UserSearchList> getSuggestions() {
        return execute(apiClient.getContactApi().getSuggestions());
    }

    // ── Generic executor ─────────────────────────────────────────────────────

    private <T> Result<T> execute(retrofit2.Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            }
            int code = response.code();
            if (code == 401) return Result.unauthorized();
            return Result.error("Lỗi " + code, code);
        } catch (IOException e) {
            return Result.networkError();
        } catch (Exception e) {
            return Result.error("Lỗi không xác định: " + e.getMessage(), 0);
        }
    }
}
