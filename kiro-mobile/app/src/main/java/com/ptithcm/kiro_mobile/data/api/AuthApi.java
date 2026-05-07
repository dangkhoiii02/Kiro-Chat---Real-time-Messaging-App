package com.ptithcm.kiro_mobile.data.api;

import com.ptithcm.kiro_mobile.data.model.auth.TokenResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface AuthApi {

    /**
     * Keycloak password grant — obtain tokens.
     * Uses @Url so the full Keycloak URL is passed at call time.
     */
    @FormUrlEncoded
    @POST
    Call<TokenResponse> getToken(
            @Url String url,
            @Field("client_id")  String clientId,
            @Field("grant_type") String grantType,
            @Field("username")   String username,
            @Field("password")   String password
    );

    /**
     * Keycloak refresh grant.
     */
    @FormUrlEncoded
    @POST
    Call<TokenResponse> refreshToken(
            @Url String url,
            @Field("client_id")     String clientId,
            @Field("grant_type")    String grantType,
            @Field("refresh_token") String refreshToken
    );
}
