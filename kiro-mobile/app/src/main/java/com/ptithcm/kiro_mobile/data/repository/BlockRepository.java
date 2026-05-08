package com.ptithcm.kiro_mobile.data.repository;

import android.content.Context;
import com.ptithcm.kiro_mobile.data.api.ApiClient;
import com.ptithcm.kiro_mobile.data.model.contact.BlockedUserList;
import com.ptithcm.kiro_mobile.util.Result;
import retrofit2.Response;

public class BlockRepository {
    private final ApiClient apiClient;

    public BlockRepository(Context context) {
        this.apiClient = ApiClient.getInstance(context);
    }

    public Result<BlockedUserList> getBlockedUsers() {
        try {
            Response<BlockedUserList> response = apiClient.getBlockApi().getBlockedUsers().execute();
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            }
            return Result.error("Failed to load block list", response.code());
        } catch (Exception e) {
            return Result.networkError();
        }
    }

    public Result<Void> blockUser(String userId) {
        try {
            Response<Void> response = apiClient.getBlockApi().blockUser(userId).execute();
            if (response.isSuccessful()) {
                return Result.success(null);
            }
            return Result.error("Failed to block user", response.code());
        } catch (Exception e) {
            return Result.networkError();
        }
    }

    public Result<Void> unblockUser(String userId) {
        try {
            Response<Void> response = apiClient.getBlockApi().unblockUser(userId).execute();
            if (response.isSuccessful()) {
                return Result.success(null);
            }
            return Result.error("Failed to unblock user", response.code());
        } catch (Exception e) {
            return Result.networkError();
        }
    }
}
