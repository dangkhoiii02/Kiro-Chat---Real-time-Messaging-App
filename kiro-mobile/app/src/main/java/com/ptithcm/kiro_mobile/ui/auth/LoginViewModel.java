package com.ptithcm.kiro_mobile.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.auth.AuthRepository;
import com.ptithcm.kiro_mobile.data.model.auth.TokenResponse;
import com.ptithcm.kiro_mobile.util.Result;
import com.ptithcm.kiro_mobile.util.SingleLiveEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Result<TokenResponse>> loginResult = new MutableLiveData<>();
    private final SingleLiveEvent<Void> navigateToMain = new SingleLiveEvent<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
    }

    public LiveData<Result<TokenResponse>> getLoginResult() { return loginResult; }
    public LiveData<Void> getNavigateToMain()               { return navigateToMain; }

    public void login(String username, String password) {
        if (username.trim().isEmpty() || password.isEmpty()) {
            loginResult.setValue(Result.error("Vui lòng nhập đầy đủ thông tin.", 0));
            return;
        }

        loginResult.setValue(Result.loading());

        executor.execute(() -> {
            Result<TokenResponse> result = authRepository.login(username.trim(), password);
            loginResult.postValue(result);
            if (result.isSuccess()) {
                // Small delay to ensure SharedPreferences is flushed before MainActivity reads it
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                navigateToMain.postValue(null);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
