package com.ptithcm.kiro_mobile.data.repository;

import android.content.Context;

import com.ptithcm.kiro_mobile.data.api.ApiClient;
import com.ptithcm.kiro_mobile.data.model.chat.ConversationList;
import com.ptithcm.kiro_mobile.data.model.chat.ConversationReference;
import com.ptithcm.kiro_mobile.data.model.chat.CreateGroupRequest;
import com.ptithcm.kiro_mobile.data.model.chat.MessageList;
import com.ptithcm.kiro_mobile.util.Result;

import java.io.IOException;

import retrofit2.Response;
import java.util.List;

public class ConversationRepository {

    private final ApiClient apiClient;

    public ConversationRepository(Context context) {
        this.apiClient = ApiClient.getInstance(context);
    }

    public Result<ConversationList> getConversations(int page, int size) {
        return execute(apiClient.getConversationApi().getConversations(page, size));
    }

    public Result<ConversationReference> getConversationByUser(String userId) {
        return execute(apiClient.getConversationApi().getConversationByUser(userId));
    }

    public Result<ConversationReference> createDirectConversation(String userId) {
        return execute(apiClient.getConversationApi().createDirectConversation(userId));
    }

    public Result<ConversationReference> createGroupConversation(String groupName, List<String> memberIds) {
        return execute(apiClient.getConversationApi()
                .createGroupConversation(new CreateGroupRequest(groupName, "", memberIds)));
    }

    public Result<MessageList> getMessages(String conversationId, int limit, String before) {
        return execute(apiClient.getConversationApi().getMessages(conversationId, limit, before));
    }

    private <T> Result<T> execute(retrofit2.Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            }
            int code = response.code();
            String errorBody = "";
            try { if (response.errorBody() != null) errorBody = response.errorBody().string(); }
            catch (Exception ignored) {}
            android.util.Log.w("ConvRepo", "HTTP " + code + " url=" + call.request().url() + " body=" + errorBody);
            if (code == 401) return Result.unauthorized();
            if (response.isSuccessful()) return Result.success(null); // 200 empty body
            return Result.error("Lỗi " + code, code);
        } catch (IOException e) {
            android.util.Log.e("ConvRepo", "Network error", e);
            return Result.networkError();
        } catch (Exception e) {
            android.util.Log.e("ConvRepo", "Unknown error", e);
            return Result.error("Lỗi không xác định: " + e.getMessage(), 0);
        }
    }
}
