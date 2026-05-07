package com.ptithcm.kiro_mobile.data.api;

import android.content.Context;

import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.auth.AuthInterceptor;
import com.ptithcm.kiro_mobile.data.auth.TokenAuthenticator;
import com.ptithcm.kiro_mobile.data.auth.TokenManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static ApiClient instance;

    private final Retrofit retrofit;
    private final AuthApi authApi;
    private final UserApi userApi;
    private final ContactApi contactApi;
    private final ConversationApi conversationApi;
    private final MessageApi messageApi;

    private ApiClient(Context context) {
        TokenManager tokenManager = TokenManager.getInstance(context);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Plain client for auth (no token interceptor — avoids circular dependency)
        OkHttpClient plainClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit plainRetrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.BASE_API_URL)
                .client(plainClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authApi = plainRetrofit.create(AuthApi.class);

        // Authenticated client
        OkHttpClient authClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenManager))
                .authenticator(new TokenAuthenticator(tokenManager, authApi))
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.BASE_API_URL)
                .client(authClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        userApi         = retrofit.create(UserApi.class);
        contactApi      = retrofit.create(ContactApi.class);
        conversationApi = retrofit.create(ConversationApi.class);
        messageApi      = retrofit.create(MessageApi.class);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }

    public AuthApi         getAuthApi()         { return authApi; }
    public UserApi         getUserApi()         { return userApi; }
    public ContactApi      getContactApi()      { return contactApi; }
    public ConversationApi getConversationApi() { return conversationApi; }
    public MessageApi      getMessageApi()      { return messageApi; }

    public <T> T create(Class<T> service) {
        return retrofit.create(service);
    }
}
