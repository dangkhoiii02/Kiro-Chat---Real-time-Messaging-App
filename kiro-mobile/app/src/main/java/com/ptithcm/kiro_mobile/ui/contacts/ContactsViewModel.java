package com.ptithcm.kiro_mobile.ui.contacts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.model.contact.ContactRequest;
import com.ptithcm.kiro_mobile.data.model.contact.ContactRequestList;
import com.ptithcm.kiro_mobile.data.model.contact.FriendList;
import com.ptithcm.kiro_mobile.data.model.contact.UserSearchList;
import com.ptithcm.kiro_mobile.data.model.user.PublicProfile;
import com.ptithcm.kiro_mobile.data.repository.ContactRepository;
import com.ptithcm.kiro_mobile.util.Result;
import com.ptithcm.kiro_mobile.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactsViewModel extends AndroidViewModel {

    private final ContactRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Result<List<PublicProfile>>> contacts =
            new MutableLiveData<>();
    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<PublicProfile> openConversation = new SingleLiveEvent<>();

    public ContactsViewModel(@NonNull Application application) {
        super(application);
        repository = new ContactRepository(application);
    }

    public LiveData<Result<List<PublicProfile>>> getContacts() { return contacts; }
    public LiveData<String> getToastMessage()                  { return toastMessage; }
    public LiveData<PublicProfile> getOpenConversation()        { return openConversation; }

    public void loadFriends(String query) {
        contacts.setValue(Result.<List<PublicProfile>>loading());
        executor.execute(() -> {
            Result<FriendList> result = repository.searchFriends(query != null ? query : "");
            if (result.isSuccess()) {
                List<PublicProfile> list = new ArrayList<>();
                if (result.getData().getFriends() != null &&
                    result.getData().getFriends().getContent() != null) {
                    list.addAll(result.getData().getFriends().getContent());
                }
                contacts.postValue(Result.success(list));
            } else {
                contacts.postValue(Result.<List<PublicProfile>>error(
                        result.getMessage(), result.getCode()));
            }
        });
    }

    public void searchUsers(String query) {
        contacts.setValue(Result.<List<PublicProfile>>loading());
        executor.execute(() -> {
            Result<UserSearchList> result = repository.searchUsers(query);
            if (result.isSuccess()) {
                List<PublicProfile> list = new ArrayList<>();
                UserSearchList data = result.getData();
                if (data.getUsers() != null && data.getUsers().getContent() != null) {
                    list.addAll(data.getUsers().getContent());
                } else if (data.getContent() != null) {
                    list.addAll(data.getContent());
                }
                contacts.postValue(Result.success(list));
            } else {
                contacts.postValue(Result.<List<PublicProfile>>error(
                        result.getMessage(), result.getCode()));
            }
        });
    }

    public void loadPendingRequests() {
        contacts.setValue(Result.<List<PublicProfile>>loading());
        executor.execute(() -> {
            Result<ContactRequestList> result = repository.getPendingRequests();
            if (result.isSuccess()) {
                List<PublicProfile> list = new ArrayList<>();
                ContactRequestList data = result.getData();
                if (data.getRequests() != null && data.getRequests().getContent() != null) {
                    for (ContactRequest req : data.getRequests().getContent()) {
                        list.add(mapRequestToProfile(req));
                    }
                } else if (data.getContent() != null) {
                    for (ContactRequest req : data.getContent()) {
                        list.add(mapRequestToProfile(req));
                    }
                }
                contacts.postValue(Result.success(list));
            } else {
                contacts.postValue(Result.<List<PublicProfile>>error(
                        result.getMessage(), result.getCode()));
            }
        });
    }

    public void sendFriendRequest(String userId) {
        executor.execute(() -> {
            Result<?> result = repository.sendFriendRequest(userId);
            toastMessage.postValue(result.isSuccess()
                    ? "Đã gửi lời mời kết bạn"
                    : result.getMessage());
        });
    }

    public void unsendRequest(String userId) {
        executor.execute(() -> {
            Result<?> result = repository.unsendFriendRequest(userId);
            toastMessage.postValue(result.isSuccess()
                    ? "Đã huỷ lời mời"
                    : result.getMessage());
        });
    }

    public void acceptRequest(String userId) {
        executor.execute(() -> {
            Result<?> result = repository.acceptRequest(userId);
            if (result.isSuccess()) {
                toastMessage.postValue("Đã chấp nhận lời mời");
                loadPendingRequests();
            } else {
                toastMessage.postValue(result.getMessage());
            }
        });
    }

    public void rejectRequest(String userId) {
        executor.execute(() -> {
            Result<?> result = repository.rejectRequest(userId);
            if (result.isSuccess()) {
                toastMessage.postValue("Đã từ chối lời mời");
                loadPendingRequests();
            } else {
                toastMessage.postValue(result.getMessage());
            }
        });
    }

    public void removeFriend(String userId) {
        executor.execute(() -> {
            Result<?> result = repository.removeFriend(userId);
            if (result.isSuccess()) {
                toastMessage.postValue("Đã huỷ kết bạn");
                loadFriends("");
            } else {
                toastMessage.postValue(result.getMessage());
            }
        });
    }

    public void openMessage(PublicProfile profile) {
        openConversation.setValue(profile);
    }

    private PublicProfile mapRequestToProfile(ContactRequest req) {
        PublicProfile p = new PublicProfile();
        p.setUserId(req.getSenderId());
        p.setUsername(req.getSenderUsername());
        p.setFullname(req.getSenderName());
        p.setProfilePictureUrl(req.getSenderAvatarUrl());
        return p;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
