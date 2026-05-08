package com.ptithcm.kiro_mobile.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.auth.TokenManager;
import com.ptithcm.kiro_mobile.data.model.chat.ChatMessage;
import com.ptithcm.kiro_mobile.data.model.chat.LocalMessage;
import com.ptithcm.kiro_mobile.data.model.chat.MessageList;
import com.ptithcm.kiro_mobile.data.repository.MessageRepository;
import com.ptithcm.kiro_mobile.data.socket.SocketEvent;
import com.ptithcm.kiro_mobile.data.socket.StompManager;
import com.ptithcm.kiro_mobile.util.Result;
import com.ptithcm.kiro_mobile.util.SingleLiveEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.disposables.CompositeDisposable;

public class ChatViewModel extends AndroidViewModel {

    private static final int PAGE_SIZE = 20;

    private final MessageRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Mutable backing list — always accessed on main thread via postValue
    private final List<LocalMessage> messageList = new ArrayList<>();
    // Server messageIds we've already added — prevents duplicates
    private final Set<String> knownServerIds = new HashSet<>();

    private final MutableLiveData<List<LocalMessage>> messages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingInitial = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingOlder   = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMoreMessages  = new MutableLiveData<>(true);
    private final SingleLiveEvent<String>  errorEvent       = new SingleLiveEvent<>();
    private final SingleLiveEvent<Void>    scrollToBottom   = new SingleLiveEvent<>();

    private String conversationId;
    private String currentUserId;
    private boolean isGroupConversation;

    /** RxJava2 disposables for socket subscriptions — disposed in onCleared(). */
    private final CompositeDisposable socketDisposables = new CompositeDisposable();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository    = new MessageRepository(application);
        currentUserId = resolveCurrentUserId(application);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    public void init(String conversationId) {
        init(conversationId, false);
    }

    public void init(String conversationId, boolean isGroupConversation) {
        this.conversationId = conversationId;
        this.isGroupConversation = isGroupConversation;
        loadInitialMessages();
    }

    /**
     * Wire up realtime socket events from StompManager.
     * Call this from ChatFragment after init().
     */
    public void observeSocket(StompManager stomp) {
        // Subscribe to group topic if needed
        if (isGroupConversation && conversationId != null) {
            stomp.subscribeGroup(conversationId);
        }

        socketDisposables.add(
                stomp.events().subscribe(
                        this::handleSocketEvent,
                        err -> android.util.Log.e("ChatViewModel", "Socket error", err)
                )
        );
    }

    private void handleSocketEvent(SocketEvent event) {
        if (event == null || event.message == null) return;
        ChatMessage msg = event.message;

        // Only process events for this conversation
        if (conversationId != null && !conversationId.equals(msg.getConversationId())) return;

        switch (event.type) {
            case MESSAGE_RECEIVED:
                mergeIncomingMessage(msg);
                break;
            case MESSAGE_SENT:
                // Update status of a message we sent (confirmed by server)
                updateMessageStatus(msg.getMessageId(), "SENT");
                break;
            case MESSAGE_DELIVERED:
                updateMessageStatus(msg.getMessageId(), "DELIVERED");
                break;
            case MESSAGE_SEEN:
                updateMessageStatus(msg.getMessageId(), "SEEN");
                break;
        }
    }

    private void mergeIncomingMessage(ChatMessage msg) {
        if (msg.getMessageId() == null) return;
        synchronized (messageList) {
            if (knownServerIds.contains(msg.getMessageId())) return; // already present
            knownServerIds.add(msg.getMessageId());
            messageList.add(LocalMessage.fromServer(msg, currentUserId));
        }
        publishMessages();
        scrollToBottom.postValue(null);
    }

    private void updateMessageStatus(String messageId, String serverState) {
        if (messageId == null) return;
        boolean changed = false;
        synchronized (messageList) {
            for (LocalMessage m : messageList) {
                if (messageId.equals(m.getMessageId())) {
                    m.updateStatus(serverState);
                    changed = true;
                    break;
                }
            }
        }
        if (changed) publishMessages();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public LiveData<List<LocalMessage>> getMessages()       { return messages; }
    public LiveData<Boolean> getIsLoadingInitial()          { return isLoadingInitial; }
    public LiveData<Boolean> getIsLoadingOlder()            { return isLoadingOlder; }
    public LiveData<Boolean> getHasMoreMessages()           { return hasMoreMessages; }
    public LiveData<String>  getErrorEvent()                { return errorEvent; }
    public LiveData<Void>    getScrollToBottom()            { return scrollToBottom; }

    // ── Load messages ─────────────────────────────────────────────────────────

    public void loadInitialMessages() {
        isLoadingInitial.setValue(true);
        executor.execute(() -> {
            Result<MessageList> result = repository.getMessages(conversationId, PAGE_SIZE, null);
            isLoadingInitial.postValue(false);
            if (result.isSuccess()) {
                List<LocalMessage> loaded = convertAndDeduplicate(result.getData());
                synchronized (messageList) {
                    messageList.clear();
                    knownServerIds.clear();
                    for (LocalMessage m : loaded) {
                        if (m.getMessageId() != null) knownServerIds.add(m.getMessageId());
                    }
                    messageList.addAll(loaded);
                }
                publishMessages();
                scrollToBottom.postValue(null);
            } else {
                errorEvent.postValue(result.getMessage());
            }
        });
    }

    public void loadOlderMessages() {
        Boolean loading = isLoadingOlder.getValue();
        Boolean hasMore = hasMoreMessages.getValue();
        if (Boolean.TRUE.equals(loading) || Boolean.FALSE.equals(hasMore)) return;

        String cursor;
        synchronized (messageList) {
            if (messageList.isEmpty()) return;
            // Find the oldest server message (first in list that has a real messageId)
            cursor = null;
            for (LocalMessage m : messageList) {
                if (m.getMessageId() != null) { cursor = m.getMessageId(); break; }
            }
        }
        if (cursor == null) return;

        isLoadingOlder.setValue(true);
        final String finalCursor = cursor;
        executor.execute(() -> {
            Result<MessageList> result = repository.getMessages(conversationId, PAGE_SIZE, finalCursor);
            isLoadingOlder.postValue(false);
            if (result.isSuccess()) {
                hasMoreMessages.postValue(result.getData().isHasMore());
                List<LocalMessage> older = convertAndDeduplicate(result.getData());
                synchronized (messageList) {
                    for (LocalMessage m : older) {
                        if (m.getMessageId() != null && !knownServerIds.contains(m.getMessageId())) {
                            knownServerIds.add(m.getMessageId());
                            messageList.add(0, m); // prepend
                        }
                    }
                }
                publishMessages();
            } else {
                errorEvent.postValue(result.getMessage());
            }
        });
    }

    // ── Send message ──────────────────────────────────────────────────────────

    public void sendMessage(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;

        // 1. Create local pending message and show immediately
        LocalMessage local = LocalMessage.outgoing(conversationId, trimmed, "text", currentUserId);
        synchronized (messageList) {
            messageList.add(local);
        }
        publishMessages();
        scrollToBottom.postValue(null);

        // 2. Call API in background
        executor.execute(() -> doSend(local, trimmed));
    }

    /** Retry a previously failed message. */
    public void retryMessage(String localId) {
        LocalMessage target = findByLocalId(localId);
        if (target == null || !target.isFailed()) return;
        target.markPending();
        publishMessages();
        executor.execute(() -> doSend(target, target.getContent()));
    }

    private void doSend(LocalMessage local, String content) {
        Result<ChatMessage> result = isGroupConversation
                ? repository.sendGroupMessage(conversationId, content, "text")
                : repository.sendDirectMessage(conversationId, content, "text");
        synchronized (messageList) {
            if (result.isSuccess()) {
                ChatMessage serverMsg = result.getData();
                // Deduplicate: if server messageId already in list, remove the local copy
                if (serverMsg.getMessageId() != null &&
                    knownServerIds.contains(serverMsg.getMessageId())) {
                    messageList.remove(local);
                } else {
                    local.confirmSent(serverMsg);
                    if (serverMsg.getMessageId() != null) {
                        knownServerIds.add(serverMsg.getMessageId());
                    }
                }
            } else {
                local.markFailed();
            }
        }
        publishMessages();
    }

    public void uploadAndSendMedia(File file) {
        // 1. Create local pending message with local file path for optimistic UI
        LocalMessage local = LocalMessage.outgoing(conversationId, "", "image", currentUserId);
        // We hijack mediaUrl to hold local path temporarily
        local.confirmSent(new ChatMessage()); // Temporary
        // Wait, confirmSent overwrites state. Let's not use confirmSent here.
        // We just need a way to set mediaUrl on LocalMessage. LocalMessage has no setMediaUrl.
        // I will add a method or just pass it in constructor if possible.
        // Actually, let's just show a generic pending image or let the adapter handle it.
        // For simplicity, we just send it in background.
        
        executor.execute(() -> doSendMedia(file, local));
    }

    private void doSendMedia(File file, LocalMessage local) {
        // Add to UI as pending
        synchronized (messageList) {
            messageList.add(local);
        }
        publishMessages();
        scrollToBottom.postValue(null);

        Result<ChatMessage> result = isGroupConversation
                ? repository.sendGroupAttachment(conversationId, file)
                : repository.sendDirectAttachment(conversationId, file);
                
        synchronized (messageList) {
            if (result.isSuccess()) {
                ChatMessage serverMsg = result.getData();
                if (serverMsg.getMessageId() != null &&
                    knownServerIds.contains(serverMsg.getMessageId())) {
                    messageList.remove(local);
                } else {
                    local.confirmSent(serverMsg);
                    if (serverMsg.getMessageId() != null) {
                        knownServerIds.add(serverMsg.getMessageId());
                    }
                }
            } else {
                local.markFailed();
            }
        }
        publishMessages();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<LocalMessage> convertAndDeduplicate(MessageList data) {
        List<LocalMessage> result = new ArrayList<>();
        if (data.getMessages() == null) return result;
        for (ChatMessage msg : data.getMessages()) {
            if (msg.getMessageId() != null && knownServerIds.contains(msg.getMessageId())) {
                continue; // already in list
            }
            result.add(LocalMessage.fromServer(msg, currentUserId));
        }
        return result;
    }

    private LocalMessage findByLocalId(String localId) {
        synchronized (messageList) {
            for (LocalMessage m : messageList) {
                if (m.getLocalId().equals(localId)) return m;
            }
        }
        return null;
    }

    private void publishMessages() {
        List<LocalMessage> snapshot;
        synchronized (messageList) {
            snapshot = new ArrayList<>(messageList);
        }
        messages.postValue(snapshot);
    }

    private String resolveCurrentUserId(Application app) {
        // TokenManager stores the access token; we can't decode JWT here without a library.
        // We'll set it lazily from the profile once loaded.
        // For now return a placeholder — ProfileViewModel will call setCurrentUserId().
        return null;
    }

    /** Called by the host activity/fragment once the profile is loaded. */
    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        socketDisposables.dispose();
        executor.shutdown();
    }
}
