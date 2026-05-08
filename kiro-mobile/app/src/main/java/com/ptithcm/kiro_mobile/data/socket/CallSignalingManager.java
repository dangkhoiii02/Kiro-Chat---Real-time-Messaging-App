package com.ptithcm.kiro_mobile.data.socket;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.ptithcm.kiro_mobile.data.model.call.CallSignalMessage;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import ua.naiksoftware.stomp.StompClient;

/**
 * Singleton that manages call signaling via STOMP WebSocket.
 * Delegates actual STOMP send/subscribe to StompManager to avoid
 * "StompClient is not connected" errors.
 */
public class CallSignalingManager {

    private static final String TAG = "CallSignalingManager";

    public static final String DEST_SUBSCRIBE = "/user/queue/calls.signal";
    public static final String DEST_PUBLISH   = "/app/calls.signal";

    private static volatile CallSignalingManager instance;

    private final Gson gson = new Gson();
    private final PublishSubject<CallSignalMessage> signalSubject = PublishSubject.create();

    // Keep a reference to the StompClient for subscribing, but always
    // use StompManager for sending so we benefit from its reconnect logic.
    private Context appContext;
    private Disposable subscription;

    private CallSignalingManager() {}

    public static CallSignalingManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CallSignalingManager.class) {
                if (instance == null) {
                    instance = new CallSignalingManager();
                }
            }
        }
        if (instance.appContext == null) {
            instance.appContext = context.getApplicationContext();
        }
        return instance;
    }

    /**
     * Called from StompManager once the STOMP connection is established.
     * Subscribes to the call signal queue.
     */
    public void subscribeCallSignals(StompClient stompClient) {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }

        if (stompClient != null && stompClient.isConnected()) {
            subscription = stompClient.topic(DEST_SUBSCRIBE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            msg -> handlePayload(msg.getPayload()),
                            err -> Log.e(TAG, "Error subscribing to call signals", err)
                    );
            Log.d(TAG, "Subscribed to call signals: " + DEST_SUBSCRIBE);
        } else {
            Log.w(TAG, "subscribeCallSignals: stompClient not connected yet");
        }
    }

    private void handlePayload(String payload) {
        try {
            CallSignalMessage message = gson.fromJson(payload, CallSignalMessage.class);
            if (message != null) {
                Log.d(TAG, "Received call signal: " + message.getType()
                        + " conv=" + message.getConversationId());
                signalSubject.onNext(message);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse call signal payload: " + payload, e);
        }
    }

    /**
     * Publishes a signal via StompManager (which handles reconnect internally).
     */
    public void sendSignal(CallSignalMessage message) {
        if (appContext == null) {
            Log.e(TAG, "sendSignal: appContext is null");
            return;
        }

        StompManager stomp = StompManager.getInstance(appContext);
        stomp.sendCallSignal(gson.toJson(message));
    }

    /**
     * Observable stream of incoming signals, emits on main thread.
     */
    public Observable<CallSignalMessage> signals() {
        return signalSubject.observeOn(AndroidSchedulers.mainThread());
    }
}
