package com.ptithcm.kiro_mobile.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.model.user.UserProfile;
import com.ptithcm.kiro_mobile.data.repository.UserRepository;
import com.ptithcm.kiro_mobile.util.Result;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Result<UserProfile>> profileResult = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    public LiveData<Result<UserProfile>> getProfileResult() { return profileResult; }

    public void loadProfile() {
        profileResult.setValue(Result.loading());
        executor.execute(() -> {
            Result<UserProfile> result = userRepository.getMyProfile();
            profileResult.postValue(result);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
