package com.ptithcm.kiro_mobile.data.api;

import com.ptithcm.kiro_mobile.data.model.chat.ChatMessage;
import com.ptithcm.kiro_mobile.data.model.chat.SendMessageRequest;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface MessageApi {

    @POST("messages/individual/send")
    Call<ChatMessage> sendDirectMessage(@Body SendMessageRequest request);

    @POST("messages/group/send")
    Call<ChatMessage> sendGroupMessage(@Body SendMessageRequest request);

    @Multipart
    @POST("messages/individual/send-attachment")
    Call<ChatMessage> sendDirectAttachment(
            @Part("conversationId") RequestBody conversationId,
            @Part("replyToMessageId") RequestBody replyToMessageId,
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("messages/group/send-attachment")
    Call<ChatMessage> sendGroupAttachment(
            @Part("conversationId") RequestBody conversationId,
            @Part("replyToMessageId") RequestBody replyToMessageId,
            @Part MultipartBody.Part file
    );
}
