package com.ptithcm.kiro_mobile.ui.call;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.kiro_mobile.data.api.ApiClient;
import com.ptithcm.kiro_mobile.data.model.call.CallTokenResponse;
import com.ptithcm.kiro_mobile.util.SingleLiveEvent;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manages call state and token fetching.
 * LiveKit Room lifecycle is handled directly in VideoCallActivity
 * to avoid Kotlin coroutine bridging complexity.
 */
public class VideoCallViewModel extends AndroidViewModel {

    private static final String TAG = "VideoCallViewModel";

    private final MutableLiveData<CallUiState> uiState = new MutableLiveData<>(new CallUiState());
    private final SingleLiveEvent<String> errorEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> tokenReady = new SingleLiveEvent<>();

    public VideoCallViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<CallUiState> getUiState()     { return uiState; }
    public LiveData<String>      getErrorEvent()  { return errorEvent; }
    /** Emits the LiveKit JWT token once fetched successfully. */
    public LiveData<String>      getTokenReady()  { return tokenReady; }

    // ── Token fetch ───────────────────────────────────────────────────────────

    public void fetchToken(String conversationId) {
        updateState(s -> { s.isConnecting = true; return s; });

        ApiClient.getInstance(getApplication())
                .getCallApi()
                .getToken(conversationId, "android")
                .enqueue(new Callback<CallTokenResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CallTokenResponse> call,
                                           @NonNull Response<CallTokenResponse> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getToken() != null) {
                            tokenReady.setValue(response.body().getToken());
                        } else {
                            handleError("Lỗi cấp phát token: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<CallTokenResponse> call,
                                          @NonNull Throwable t) {
                        handleError("Không thể kết nối đến máy chủ: " + t.getMessage());
                    }
                });
    }

    // ── UI state helpers ──────────────────────────────────────────────────────

    public void onConnected(boolean withVideo) {
        updateState(s -> {
            s.isConnecting    = false;
            s.isAudioEnabled  = true;
            s.isVideoEnabled  = withVideo;
            return s;
        });
    }

    public void onDisconnected() {
        updateState(s -> { s.isConnecting = false; return s; });
    }

    public void setVideoEnabled(boolean enabled) {
        updateState(s -> { s.isVideoEnabled = enabled; return s; });
    }

    public void setAudioEnabled(boolean enabled) {
        updateState(s -> { s.isAudioEnabled = enabled; return s; });
    }

    public void setSpeakerOn(boolean on) {
        updateState(s -> { s.isSpeakerOn = on; return s; });
    }

    public void setRemoteVideoActive(boolean active) {
        updateState(s -> { s.remoteVideoActive = active; return s; });
    }

    public void updateDuration(int seconds) {
        updateState(s -> { s.durationSeconds = seconds; return s; });
    }

    public void onPermissionsDeniedAudio() {
        handleError("Vui lòng cấp quyền micro để tiếp tục cuộc gọi");
    }

    public void onCameraDenied() {
        errorEvent.setValue("Quyền camera bị từ chối, cuộc gọi chuyển sang âm thanh");
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void handleError(String message) {
        Log.e(TAG, message);
        errorEvent.setValue(message);
        updateState(s -> { s.isConnecting = false; return s; });
    }

    private interface StateTransform {
        CallUiState apply(CallUiState state);
    }

    private void updateState(StateTransform transform) {
        CallUiState current = uiState.getValue();
        if (current == null) current = new CallUiState();
        uiState.setValue(transform.apply(current.copy()));
    }
}
