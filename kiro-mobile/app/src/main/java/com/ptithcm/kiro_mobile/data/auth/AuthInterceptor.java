package com.ptithcm.kiro_mobile.data.auth;

import androidx.annotation.NonNull;

import com.ptithcm.kiro_mobile.config.AppConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Attaches "Authorization: Bearer <token>" to every request
 * that is NOT a Keycloak token endpoint.
 */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String url = original.url().toString();

        // Skip auth header for Keycloak token endpoint
        if (url.contains("/protocol/openid-connect/token")) {
            return chain.proceed(original);
        }

        String token = tokenManager.getAccessToken();
        android.util.Log.d("AuthInterceptor", "url=" + url.substring(url.lastIndexOf("/api")) + " token=" + (token != null ? "present("+token.length()+"chars)" : "NULL"));
        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }

        Request authenticated = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(authenticated);
    }
}
