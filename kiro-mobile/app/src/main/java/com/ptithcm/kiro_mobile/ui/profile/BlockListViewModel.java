package com.ptithcm.kiro_mobile.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.model.contact.BlockedUser;
import com.ptithcm.kiro_mobile.data.model.contact.BlockedUserList;
import com.ptithcm.kiro_mobile.data.repository.BlockRepository;
import com.ptithcm.kiro_mobile.util.Result;
import com.ptithcm.kiro_mobile.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockListViewModel extends AndroidViewModel {

    private final BlockRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Result<List<BlockedUser>>> blockList = new MutableLiveData<>();
    private final SingleLiveEvent<Result<Void>> unblockEvent = new SingleLiveEvent<>();

    public BlockListViewModel(@NonNull Application application) {
        super(application);
        repository = new BlockRepository(application);
    }

    public LiveData<Result<List<BlockedUser>>> getBlockList() {
        return blockList;
    }

    public LiveData<Result<Void>> getUnblockEvent() {
        return unblockEvent;
    }

    public void loadBlockedUsers() {
        blockList.setValue(Result.loading());
        executor.execute(() -> {
            Result<BlockedUserList> result = repository.getBlockedUsers();
            if (result.isSuccess() && result.getData() != null && result.getData().getBlockedUsers() != null) {
                blockList.postValue(Result.success(new ArrayList<>(result.getData().getBlockedUsers())));
            } else if (result.isSuccess()) {
                blockList.postValue(Result.success(new ArrayList<>()));
            } else {
                blockList.postValue(Result.error(result.getMessage(), result.getCode()));
            }
        });
    }

    public void unblockUser(String userId) {
        executor.execute(() -> {
            Result<Void> result = repository.unblockUser(userId);
            unblockEvent.postValue(result);
            if (result.isSuccess()) {
                // Refresh list
                loadBlockedUsers();
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
