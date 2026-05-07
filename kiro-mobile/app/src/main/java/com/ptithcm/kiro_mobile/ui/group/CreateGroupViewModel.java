package com.ptithcm.kiro_mobile.ui.group;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.model.chat.ConversationReference;
import com.ptithcm.kiro_mobile.data.model.contact.FriendList;
import com.ptithcm.kiro_mobile.data.model.user.PublicProfile;
import com.ptithcm.kiro_mobile.data.repository.ContactRepository;
import com.ptithcm.kiro_mobile.data.repository.ConversationRepository;
import com.ptithcm.kiro_mobile.util.Result;
import com.ptithcm.kiro_mobile.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateGroupViewModel extends AndroidViewModel {
    private final ContactRepository contactRepository;
    private final ConversationRepository conversationRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Result<List<PublicProfile>>> friends = new MutableLiveData<>();
    private final MutableLiveData<Boolean> creating = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> toast = new SingleLiveEvent<>();
    private final SingleLiveEvent<ConversationReference> created = new SingleLiveEvent<>();

    public CreateGroupViewModel(@NonNull Application application) {
        super(application);
        contactRepository = new ContactRepository(application);
        conversationRepository = new ConversationRepository(application);
    }

    public LiveData<Result<List<PublicProfile>>> getFriends() { return friends; }
    public LiveData<Boolean> getCreating() { return creating; }
    public LiveData<String> getToast() { return toast; }
    public LiveData<ConversationReference> getCreated() { return created; }

    public void loadFriends() {
        friends.setValue(Result.loading());
        executor.execute(() -> {
            Result<FriendList> result = contactRepository.searchFriends("");
            if (result.isSuccess()) {
                List<PublicProfile> list = new ArrayList<>();
                if (result.getData().getFriends() != null
                        && result.getData().getFriends().getContent() != null) {
                    list.addAll(result.getData().getFriends().getContent());
                }
                friends.postValue(Result.success(list));
            } else {
                friends.postValue(Result.error(result.getMessage(), result.getCode()));
            }
        });
    }

    public void createGroup(String name, List<String> memberIds) {
        String groupName = name != null ? name.trim() : "";
        if (groupName.isEmpty()) {
            toast.setValue("Nhập tên nhóm");
            return;
        }
        if (memberIds == null || memberIds.size() < 2) {
            toast.setValue("Cần chọn ít nhất 2 thành viên");
            return;
        }

        creating.setValue(true);
        executor.execute(() -> {
            Result<ConversationReference> result =
                    conversationRepository.createGroupConversation(groupName, memberIds);
            creating.postValue(false);
            if (result.isSuccess()) {
                created.postValue(result.getData());
            } else {
                toast.postValue(result.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
