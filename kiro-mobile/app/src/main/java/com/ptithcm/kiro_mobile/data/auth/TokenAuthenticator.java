package com.ptithcm.kiro_mobile.data.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.api.AuthApi;
import com.ptithcm.kiro_mobile.data.model.auth.TokenResponse;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

/**
 * Handles 401 responses by attempting a token refresh.
 * Uses a lock to ensure only one refresh happens at a time.
 */
public class TokenAuthenticator implements Authenticator {

    private final TokenManager tokenManager;
    private final AuthApi authApi;
    private final Object lock = new Object();

    public TokenAuthenticator(TokenManager tokenManager, AuthApi authApi) {
        this.tokenManager = tokenManager;
        this.authApi = authApi;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        // Avoid infinite loops — if we already tried with a refreshed token, give up
        if (response.request().header("Authorization") != null
                && responseCount(response) >= 2) {
            tokenManager.clear();
            return null;
        }

        synchronized (lock) {
            // Another thread may have already refreshed
            String currentToken = tokenManager.getAccessToken();
            String requestToken = response.request().header("Authorization");
            if (requestToken != null && !requestToken.equals("Bearer " + currentToken)) {
                // Token was already refreshed by another thread
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + currentToken)
                        .build();
            }

            if (!tokenManager.isRefreshTokenValid()) {
                tokenManager.clear();
                return null;
            }

            // Attempt refresh
            try {
                Call<TokenResponse> call = authApi.refreshToken(
                        AppConfig.KEYCLOAK_TOKEN_URL,
                        AppConfig.KEYCLOAK_CLIENT,
                        "refresh_token",
                        tokenManager.getRefreshToken()
                );
                retrofit2.Response<TokenResponse> refreshResponse = call.execute();
                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    tokenManager.save(refreshResponse.body());
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + refreshResponse.body().getAccessToken())
                            .build();
                }
            } catch (Exception e) {
                // Refresh failed
            }

            tokenManager.clear();
            return null;
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) count++;
        return count;
    }
}
