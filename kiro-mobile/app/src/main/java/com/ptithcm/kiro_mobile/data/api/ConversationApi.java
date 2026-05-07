package com.ptithcm.kiro_mobile.data.api;

import com.ptithcm.kiro_mobile.data.model.chat.ConversationList;
import com.ptithcm.kiro_mobile.data.model.chat.ConversationReference;
import com.ptithcm.kiro_mobile.data.model.chat.CreateGroupRequest;
import com.ptithcm.kiro_mobile.data.model.chat.MessageList;

import retrofit2.http.Body;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ConversationApi {

    @GET("conversations")
    Call<ConversationList> getConversations(@Query("page") int page, @Query("size") int size);

    @GET("conversations/user/{userId}")
    Call<ConversationReference> getConversationByUser(@Path("userId") String userId);

    @POST("conversations")
    Call<ConversationReference> createDirectConversation(@Query("userId") String userId);

    @POST("conversations/group")
    Call<ConversationReference> createGroupConversation(@Body CreateGroupRequest request);

    @GET("conversations/{conversationId}/messages")
    Call<MessageList> getMessages(
            @Path("conversationId") String conversationId,
            @Query("limit") int limit,
            @Query("before") String before
    );
}
