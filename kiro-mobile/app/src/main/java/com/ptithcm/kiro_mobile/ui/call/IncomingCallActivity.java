package com.ptithcm.kiro_mobile.ui.call;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.call.CallSignalMessage;
import com.ptithcm.kiro_mobile.data.socket.CallSignalingManager;
import com.ptithcm.kiro_mobile.databinding.ActivityIncomingCallBinding;

import io.reactivex.disposables.Disposable;

public class IncomingCallActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversationId";
    public static final String EXTRA_CALLER_ID       = "callerId";
    public static final String EXTRA_CALLER_NAME     = "callerName";
    public static final String EXTRA_CALLER_AVATAR   = "callerAvatar";
    public static final String EXTRA_WITH_VIDEO      = "withVideo";

    private static final String TAG = "IncomingCallActivity";

    private ActivityIncomingCallBinding binding;
    private String conversationId;
    private String callerId;
    private boolean withVideo;
    private Disposable signalSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Show on lock screen and turn screen on
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        binding = ActivityIncomingCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID);
        callerId       = intent.getStringExtra(EXTRA_CALLER_ID);
        String callerName = intent.getStringExtra(EXTRA_CALLER_NAME);
        String callerAvatar = intent.getStringExtra(EXTRA_CALLER_AVATAR);
        withVideo      = intent.getBooleanExtra(EXTRA_WITH_VIDEO, false);

        binding.tvCallerName.setText(callerName != null ? callerName : "Unknown Caller");
        binding.tvCallType.setText(withVideo ? "Cuộc gọi video đến..." : "Cuộc gọi thoại đến...");

        if (callerAvatar != null && !callerAvatar.isEmpty()) {
            String fixedAvatar = callerAvatar
                    .replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                    .replace("http://localhost:8080", "http://10.0.2.2:8080");
            Glide.with(this)
                    .load(fixedAvatar)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivCallerAvatar);
        }

        binding.btnAccept.setOnClickListener(v -> acceptCall());
        binding.btnReject.setOnClickListener(v -> rejectCall());

        observeSignals();
    }

    private void acceptCall() {
        Log.d(TAG, "Accepting call: " + conversationId);

        // Send ANSWER signal back to caller so web/other client opens LiveKit room
        CallSignalMessage answerMsg = new CallSignalMessage();
        answerMsg.setType("ANSWER");
        answerMsg.setConversationId(conversationId);
        answerMsg.setTargetUserId(callerId);
        answerMsg.setCallerId("");
        answerMsg.setCallerName("");
        answerMsg.setWithVideo(withVideo);
        CallSignalingManager.getInstance(this).sendSignal(answerMsg);

        Intent intent = new Intent(this, VideoCallActivity.class);
        intent.putExtra(VideoCallActivity.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(VideoCallActivity.EXTRA_REMOTE_USER_ID, callerId);
        intent.putExtra(VideoCallActivity.EXTRA_WITH_VIDEO, withVideo);
        intent.putExtra(VideoCallActivity.EXTRA_CALLER_NAME, binding.tvCallerName.getText().toString());
        startActivity(intent);

        finish();
    }

    private void rejectCall() {
        Log.d(TAG, "Rejecting call: " + conversationId);
        CallSignalMessage rejectMsg = new CallSignalMessage();
        rejectMsg.setType("REJECT");
        rejectMsg.setConversationId(conversationId);
        rejectMsg.setTargetUserId(callerId);
        
        CallSignalingManager.getInstance(this).sendSignal(rejectMsg);
        finish();
    }

    private void observeSignals() {
        signalSubscription = CallSignalingManager.getInstance(this).signals().subscribe(
                signal -> {
                    if (conversationId != null && conversationId.equals(signal.getConversationId())) {
                        if ("END".equals(signal.getType()) || "REJECT".equals(signal.getType())) {
                            Log.d(TAG, "Call ended by remote before answering");
                            finish();
                        }
                    }
                },
                err -> Log.e(TAG, "Error observing signals", err)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (signalSubscription != null && !signalSubscription.isDisposed()) {
            signalSubscription.dispose();
        }
    }
}
