package com.ptithcm.kiro_mobile.data.api;

import com.ptithcm.kiro_mobile.data.model.user.UserProfile;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.Part;

public interface UserApi {

    @GET("users/me")
    Call<UserProfile> getMyProfile();

    @Multipart
    @PATCH("users/me/avatar")
    Call<UserProfile> updateAvatar(@Part MultipartBody.Part avatar);
}
