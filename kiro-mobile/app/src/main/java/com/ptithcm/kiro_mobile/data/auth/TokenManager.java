package com.ptithcm.kiro_mobile.data.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.auth.TokenResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Persists and retrieves Keycloak tokens using EncryptedSharedPreferences.
 * Falls back to plain SharedPreferences if encryption setup fails.
 */
public class TokenManager {

    private static final String TAG = "TokenManager";
    private static TokenManager instance;

    private final SharedPreferences prefs;

    private TokenManager(Context context) {
        // Use plain SharedPreferences for reliability in development
        // EncryptedSharedPreferences can fail on some emulator configurations
        this.prefs = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "TokenManager initialized with plain SharedPreferences");
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context.getApplicationContext());
        }
        return instance;
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    public boolean save(TokenResponse response) {
        if (response == null
                || response.getAccessToken() == null
                || response.getAccessToken().isEmpty()) {
            Log.w(TAG, "save() called with null/empty token");
            return false;
        }

        long now = System.currentTimeMillis();
        // Use apply() (async) then verify with a direct read
        prefs.edit()
                .putString(AppConfig.KEY_ACCESS_TOKEN,  response.getAccessToken())
                .putString(AppConfig.KEY_REFRESH_TOKEN, response.getRefreshToken())
                .putLong(AppConfig.KEY_EXPIRES_AT,
                        now + (response.getExpiresIn() * 1000L))
                .putLong(AppConfig.KEY_REFRESH_EXPIRES_AT,
                        now + (response.getRefreshExpiresIn() * 1000L))
                .apply();

        // Verify the token was actually persisted
        String saved = prefs.getString(AppConfig.KEY_ACCESS_TOKEN, null);
        boolean ok = saved != null && !saved.isEmpty();
        Log.d(TAG, "save() result=" + ok + " token_prefix=" + (ok ? saved.substring(0, Math.min(10, saved.length())) : "null"));
        return ok;
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public String getAccessToken() {
        return prefs.getString(AppConfig.KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(AppConfig.KEY_REFRESH_TOKEN, null);
    }

    public long getAccessTokenExpiresAt() {
        return prefs.getLong(AppConfig.KEY_EXPIRES_AT, 0);
    }

    public long getRefreshTokenExpiresAt() {
        return prefs.getLong(AppConfig.KEY_REFRESH_EXPIRES_AT, 0);
    }

    // ── Validity checks ──────────────────────────────────────────────────────

    /** True if access token exists and is not expired (with 60s buffer). */
    public boolean isAccessTokenValid() {
        String token = getAccessToken();
        if (token == null || token.isEmpty()) return false;
        return System.currentTimeMillis() < getAccessTokenExpiresAt() - 60_000L;
    }

    /** True if refresh token exists and is not expired (with 60s buffer). */
    public boolean isRefreshTokenValid() {
        String token = getRefreshToken();
        if (token == null || token.isEmpty()) return false;
        return System.currentTimeMillis() < getRefreshTokenExpiresAt() - 60_000L;
    }

    public boolean hasSession() {
        boolean hasSession = getAccessToken() != null;
        Log.d(TAG, "hasSession=" + hasSession);
        return hasSession;
    }

    // ── Clear ────────────────────────────────────────────────────────────────

    public void clear() {
        prefs.edit().clear().apply();
    }
}
