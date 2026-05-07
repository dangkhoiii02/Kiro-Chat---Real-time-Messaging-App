package com.ptithcm.kiro_mobile.data.repository;

import android.content.Context;

import com.ptithcm.kiro_mobile.data.api.ApiClient;
import com.ptithcm.kiro_mobile.data.model.user.UserProfile;
import com.ptithcm.kiro_mobile.util.Result;

import java.io.IOException;

import retrofit2.Response;

public class UserRepository {

    private final ApiClient apiClient;

    public UserRepository(Context context) {
        this.apiClient = ApiClient.getInstance(context);
    }

    /** Synchronous — call from background thread. */
    public Result<UserProfile> getMyProfile() {
        try {
            Response<UserProfile> response = apiClient.getUserApi().getMyProfile().execute();
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            }
            int code = response.code();
            if (code == 401) return Result.unauthorized();
            return Result.error("Không thể tải thông tin người dùng (" + code + ").", code);
        } catch (IOException e) {
            return Result.networkError();
        } catch (Exception e) {
            return Result.error("Lỗi không xác định: " + e.getMessage(), 0);
        }
    }
}
