package com.ptithcm.kiro_mobile.ui.chatlist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.model.chat.Conversation;
import com.ptithcm.kiro_mobile.data.model.chat.ConversationList;
import com.ptithcm.kiro_mobile.data.repository.ConversationRepository;
import com.ptithcm.kiro_mobile.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatListViewModel extends AndroidViewModel {

    private final ConversationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Result<List<Conversation>>> conversations = new MutableLiveData<>();

    public ChatListViewModel(@NonNull Application application) {
        super(application);
        repository = new ConversationRepository(application);
    }

    public LiveData<Result<List<Conversation>>> getConversations() { return conversations; }

    public void loadConversations() {
        conversations.setValue(Result.loading());
        executor.execute(() -> {
            Result<ConversationList> result = repository.getConversations(0, 30);
            if (result.isSuccess()) {
                List<Conversation> list = new ArrayList<>();
                if (result.getData().getConversations() != null &&
                    result.getData().getConversations().getContent() != null) {
                    list.addAll(result.getData().getConversations().getContent());
                }
                conversations.postValue(Result.success(list));
            } else {
                conversations.postValue(Result.error(result.getMessage(), result.getCode()));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
