package com.ptithcm.kiro_mobile.data.repository;

import android.content.Context;

import com.ptithcm.kiro_mobile.data.api.ApiClient;
import com.ptithcm.kiro_mobile.data.model.chat.ChatMessage;
import com.ptithcm.kiro_mobile.data.model.chat.MessageList;
import com.ptithcm.kiro_mobile.data.model.chat.SendMessageRequest;
import com.ptithcm.kiro_mobile.util.Result;

import java.io.IOException;

import retrofit2.Response;

public class MessageRepository {

    private final ApiClient apiClient;

    public MessageRepository(Context context) {
        this.apiClient = ApiClient.getInstance(context);
    }

    /**
     * Load messages for a conversation.
     * @param before cursor UUID — pass null for the first page (newest messages)
     */
    public Result<MessageList> getMessages(String conversationId, int limit, String before) {
        return execute(apiClient.getConversationApi().getMessages(conversationId, limit, before));
    }

    /** Send a direct message. Synchronous — call from background thread. */
    public Result<ChatMessage> sendDirectMessage(String conversationId,
                                                  String content,
                                                  String type) {
        SendMessageRequest req = new SendMessageRequest(conversationId, content, type);
        return execute(apiClient.getMessageApi().sendDirectMessage(req));
    }

    public Result<ChatMessage> sendGroupMessage(String conversationId,
                                                 String content,
                                                 String type) {
        SendMessageRequest req = new SendMessageRequest(conversationId, content, type);
        return execute(apiClient.getMessageApi().sendGroupMessage(req));
    }

    // ── Generic executor ─────────────────────────────────────────────────────

    private <T> Result<T> execute(retrofit2.Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                return Result.success(response.body());
            }
            int code = response.code();
            if (code == 401) return Result.unauthorized();
            return Result.error("Lỗi " + code, code);
        } catch (IOException e) {
            return Result.networkError();
        } catch (Exception e) {
            return Result.error("Lỗi không xác định: " + e.getMessage(), 0);
        }
    }
}
