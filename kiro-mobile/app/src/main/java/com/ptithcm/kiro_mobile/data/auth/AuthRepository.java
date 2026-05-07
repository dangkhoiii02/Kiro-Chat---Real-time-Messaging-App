package com.ptithcm.kiro_mobile.data.auth;

import android.content.Context;
import android.util.Log;

import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.api.ApiClient;
import com.ptithcm.kiro_mobile.data.model.auth.TokenResponse;
import com.ptithcm.kiro_mobile.util.Result;

import java.io.IOException;

import retrofit2.Response;

public class AuthRepository {
    private static final String TAG = "AuthRepository";

    private final TokenManager tokenManager;
    private final ApiClient apiClient;

    public AuthRepository(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
        this.apiClient    = ApiClient.getInstance(context);
    }

    /**
     * Synchronous login — call from a background thread.
     */
    public Result<TokenResponse> login(String username, String password) {
        try {
            Log.d(TAG, "Login request started for username=" + username);
            Response<TokenResponse> response = apiClient.getAuthApi().getToken(
                    AppConfig.KEYCLOAK_TOKEN_URL,
                    AppConfig.KEYCLOAK_CLIENT,
                    "password",
                    username,
                    password
            ).execute();

            if (response.isSuccessful() && response.body() != null) {
                boolean saved = tokenManager.save(response.body());
                if (!saved) {
                    Log.e(TAG, "Login succeeded but token could not be saved");
                    return Result.error("Không lưu được phiên đăng nhập. Vui lòng thử lại.", 0);
                }
                Log.d(TAG, "Login succeeded and token saved");
                return Result.success(response.body());
            }

            int code = response.code();
            Log.w(TAG, "Login failed with HTTP " + code);
            if (code == 400 || code == 401) {
                return Result.error("Tên đăng nhập hoặc mật khẩu không đúng.", code);
            }
            return Result.error("Lỗi máy chủ (" + code + "). Vui lòng thử lại.", code);

        } catch (IOException e) {
            Log.e(TAG, "Login network error", e);
            return Result.networkError();
        } catch (Exception e) {
            Log.e(TAG, "Login unknown error", e);
            return Result.error("Lỗi không xác định: " + e.getMessage(), 0);
        }
    }

    public void logout() {
        tokenManager.clear();
    }

    public boolean hasSession() {
        return tokenManager.hasSession();
    }
}
