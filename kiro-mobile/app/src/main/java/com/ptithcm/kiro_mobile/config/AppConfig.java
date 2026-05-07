package com.ptithcm.kiro_mobile.config;

/**
 * Central configuration for all server URLs.
 * Uses Android Emulator loopback (10.0.2.2) to reach the host machine.
 */
public final class AppConfig {

    private AppConfig() {}

    // ── Emulator URLs ────────────────────────────────────────────────────────
    public static final String BASE_API_URL      = "http://10.0.2.2:8080/api/v1/";
    public static final String KEYCLOAK_URL      = "http://10.0.2.2:9093";
    public static final String WEBSOCKET_URL     = "ws://10.0.2.2:8080/api/v1/ws";
    public static final String MINIO_PUBLIC_URL  = "http://10.0.2.2:9000";
    public static final String LIVEKIT_URL       = "ws://10.0.2.2:7880";

    // ── Keycloak ─────────────────────────────────────────────────────────────
    public static final String KEYCLOAK_REALM    = "kiro-realm";
    public static final String KEYCLOAK_CLIENT   = "kiro-mobile";

    public static final String KEYCLOAK_TOKEN_URL =
            KEYCLOAK_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token";

    // ── Token prefs key ──────────────────────────────────────────────────────
    public static final String PREFS_NAME        = "kiro_secure_prefs";
    public static final String KEY_ACCESS_TOKEN  = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_EXPIRES_AT    = "access_token_expires_at";
    public static final String KEY_REFRESH_EXPIRES_AT = "refresh_token_expires_at";
}
