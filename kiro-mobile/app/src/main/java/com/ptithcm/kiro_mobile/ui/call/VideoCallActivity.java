package com.ptithcm.kiro_mobile.ui.call;

import android.Manifest;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.call.CallSignalMessage;
import com.ptithcm.kiro_mobile.data.socket.CallSignalingManager;
import com.ptithcm.kiro_mobile.databinding.ActivityVideoCallBinding;

import io.livekit.android.room.track.RemoteVideoTrack;
import io.livekit.android.room.track.VideoTrack;
import io.reactivex.disposables.Disposable;

public class VideoCallActivity extends AppCompatActivity implements LiveKitBridge.Callback {

    public static final String EXTRA_CONVERSATION_ID = "conversationId";
    public static final String EXTRA_REMOTE_USER_ID  = "remoteUserId";
    public static final String EXTRA_WITH_VIDEO      = "withVideo";
    public static final String EXTRA_CALLER_NAME     = "callerName";
    public static final String EXTRA_CALLER_AVATAR   = "callerAvatar";
    /** true = this device initiated the call (caller), false = this device received (callee) */
    public static final String EXTRA_IS_CALLER       = "isCaller";

    private static final String TAG = "VideoCallActivity";

    private ActivityVideoCallBinding binding;
    private VideoCallViewModel viewModel;
    private LiveKitBridge liveKitBridge;

    private String conversationId;
    private String remoteUserId;
    private boolean withVideo;
    private boolean isCaller;
    private boolean permissionsGranted = false;

    private Disposable signalSubscription;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int seconds = 0;
    private boolean timerRunning = false;
    private AudioManager audioManager;

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean audioGranted = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));
            boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));

            if (!audioGranted) {
                viewModel.onPermissionsDeniedAudio();
                return;
            }
            if (withVideo && !cameraGranted) {
                withVideo = false;
                viewModel.onCameraDenied();
                binding.btnCamera.setVisibility(View.GONE);
            }
            permissionsGranted = true;

            if (isCaller) {
                // Caller: show "Đang chờ..." and wait for ANSWER signal
                binding.tvDuration.setText("Đang chờ đối phương...");
            } else {
                // Callee: connect immediately (already accepted)
                viewModel.fetchToken(conversationId);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        viewModel = new ViewModelProvider(this).get(VideoCallViewModel.class);
        liveKitBridge = new LiveKitBridge(getApplicationContext());

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        remoteUserId   = getIntent().getStringExtra(EXTRA_REMOTE_USER_ID);
        withVideo      = getIntent().getBooleanExtra(EXTRA_WITH_VIDEO, false);
        isCaller       = getIntent().getBooleanExtra(EXTRA_IS_CALLER, false);
        String name    = getIntent().getStringExtra(EXTRA_CALLER_NAME);
        String avatar  = getIntent().getStringExtra(EXTRA_CALLER_AVATAR);

        binding.tvCallerName.setText(name != null ? name : "Unknown");
        if (avatar != null && !avatar.isEmpty()) {
            String fixedAvatar = fixMinioUrl(avatar);
            Glide.with(this).load(fixedAvatar).circleCrop()
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(binding.ivRemoteAvatar);
        }

        binding.btnCamera.setVisibility(withVideo ? View.VISIBLE : View.GONE);

        setupControls();
        observeViewModel();
        observeSignals();

        if (withVideo) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            });
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
        }
    }

    private void setupControls() {
        binding.btnHangup.setOnClickListener(v -> endCall());

        binding.btnCamera.setOnClickListener(v -> {
            CallUiState state = viewModel.getUiState().getValue();
            boolean next = state == null || !state.isVideoEnabled;
            viewModel.setVideoEnabled(next);
            liveKitBridge.setCameraEnabled(next);
        });

        binding.btnMic.setOnClickListener(v -> {
            CallUiState state = viewModel.getUiState().getValue();
            boolean next = state == null || !state.isAudioEnabled;
            viewModel.setAudioEnabled(next);
            liveKitBridge.setMicrophoneEnabled(next);
        });

        binding.btnSpeaker.setOnClickListener(v -> {
            boolean isSpeakerOn = audioManager.isSpeakerphoneOn();
            audioManager.setSpeakerphoneOn(!isSpeakerOn);
            viewModel.setSpeakerOn(!isSpeakerOn);
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, state -> {
            if (state.isConnecting) {
                binding.tvDuration.setText("Đang kết nối...");
            } else if (!timerRunning) {
                binding.tvDuration.setText(formatTime(state.durationSeconds));
            }
            binding.btnCamera.setAlpha(state.isVideoEnabled ? 1.0f : 0.5f);
            binding.btnMic.setAlpha(state.isAudioEnabled ? 1.0f : 0.5f);
            binding.btnSpeaker.setAlpha(state.isSpeakerOn ? 1.0f : 0.5f);

            if (state.remoteVideoActive) {
                binding.surfaceRemote.setVisibility(View.VISIBLE);
                binding.ivRemoteAvatar.setVisibility(View.GONE);
            } else {
                binding.surfaceRemote.setVisibility(View.GONE);
                binding.ivRemoteAvatar.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorEvent().observe(this, msg -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            if (msg.contains("micro")) finish();
        });

        // When token is ready, connect via bridge
        viewModel.getTokenReady().observe(this, token ->
                liveKitBridge.connect(AppConfig.LIVEKIT_URL, token, withVideo, this));
    }

    private void observeSignals() {
        signalSubscription = CallSignalingManager.getInstance(this)
                .signals()
                .subscribe(signal -> {
                    if (conversationId == null || !conversationId.equals(signal.getConversationId())) return;
                    String type = signal.getType();

                    if ("ANSWER".equals(type) && isCaller && permissionsGranted) {
                        // Web/other device accepted — now connect to LiveKit
                        runOnUiThread(() -> {
                            binding.tvDuration.setText("Đang kết nối...");
                            viewModel.fetchToken(conversationId);
                        });
                    } else if ("END".equals(type) || "REJECT".equals(type)) {
                        runOnUiThread(() -> {
                            String msg = "REJECT".equals(type)
                                    ? "Đối phương đã từ chối cuộc gọi"
                                    : "Cuộc gọi đã kết thúc";
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                }, err -> Log.e(TAG, "Signal error", err));
    }

    private void endCall() {
        CallSignalMessage endMsg = new CallSignalMessage();
        endMsg.setType("END");
        endMsg.setConversationId(conversationId);
        endMsg.setTargetUserId(remoteUserId);
        endMsg.setCallerId("");
        endMsg.setCallerName("");
        endMsg.setWithVideo(false);
        CallSignalingManager.getInstance(this).sendSignal(endMsg);
        liveKitBridge.disconnect();
        finish();
    }

    private void startTimer() {
        if (timerRunning) return;
        timerRunning = true;
        timerHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!isFinishing()) {
                    seconds++;
                    viewModel.updateDuration(seconds);
                    binding.tvDuration.setText(formatTime(seconds));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private String formatTime(int total) {
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    // ── LiveKitBridge.Callback ────────────────────────────────────────────────

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            viewModel.onConnected(withVideo);
            startTimer();
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            viewModel.onDisconnected();
            finish();
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onRemoteVideoTrack(RemoteVideoTrack track) {
        runOnUiThread(() -> {
            viewModel.setRemoteVideoActive(true);
            track.addRenderer(binding.surfaceRemote);
        });
    }

    @Override
    public void onRemoteVideoTrackRemoved() {
        runOnUiThread(() -> viewModel.setRemoteVideoActive(false));
    }

    @Override
    public void onLocalVideoTrack(VideoTrack track) {
        runOnUiThread(() -> {
            if (track != null) {
                binding.surfaceLocal.setVisibility(View.VISIBLE);
                track.addRenderer(binding.surfaceLocal);
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String fixMinioUrl(String url) {
        if (url == null) return null;
        return url.replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                  .replace("http://localhost:8080", "http://10.0.2.2:8080");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        if (signalSubscription != null && !signalSubscription.isDisposed()) {
            signalSubscription.dispose();
        }
        liveKitBridge.release();
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
    }
}
