package com.ptithcm.kiro_mobile.ui.group;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.model.group.GroupParticipantList;
import com.ptithcm.kiro_mobile.data.model.group.GroupProfile;
import com.ptithcm.kiro_mobile.data.repository.GroupRepository;
import com.ptithcm.kiro_mobile.util.Result;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupProfileViewModel extends AndroidViewModel {

    private final GroupRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Result<GroupProfile>> groupProfile = new MutableLiveData<>();
    private final MutableLiveData<Result<GroupParticipantList>> groupParticipants = new MutableLiveData<>();

    public GroupProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new GroupRepository(application);
    }

    public LiveData<Result<GroupProfile>> getGroupProfile() { return groupProfile; }
    public LiveData<Result<GroupParticipantList>> getGroupParticipants() { return groupParticipants; }

    public void loadGroupInfo(String groupId) {
        groupProfile.setValue(Result.loading());
        groupParticipants.setValue(Result.loading());
        
        executor.execute(() -> {
            Result<GroupProfile> profileResult = repository.getGroupProfile(groupId);
            groupProfile.postValue(profileResult);
            
            Result<GroupParticipantList> participantsResult = repository.getGroupParticipants(groupId);
            groupParticipants.postValue(participantsResult);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
