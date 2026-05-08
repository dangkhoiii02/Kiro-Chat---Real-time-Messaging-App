package com.ptithcm.kiro_mobile.data.socket;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.ptithcm.kiro_mobile.config.AppConfig;
import com.ptithcm.kiro_mobile.data.model.chat.ChatMessage;
import com.ptithcm.kiro_mobile.data.model.user.PresenceUpdate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import ua.naiksoftware.stomp.dto.StompHeader;

/**
 * Singleton that manages the STOMP WebSocket connection.
 *
 * <p>Usage:
 * <pre>
 *   // Connect after login
 *   StompManager.getInstance(context).connect(accessToken);
 *
 *   // Subscribe to events
 *   StompManager.getInstance(context).events().subscribe(event -> { ... });
 *
 *   // Disconnect on logout
 *   StompManager.getInstance(context).disconnect();
 * </pre>
 */
public class StompManager {

    private static final String TAG = "StompManager";

    // Destinations
    private static final String DEST_RECEIVE   = "/user/queue/messages.receive";
    private static final String DEST_SENT      = "/user/queue/messages.sent";
    private static final String DEST_DELIVERED = "/user/queue/messages.delivered";
    private static final String DEST_SEEN      = "/user/queue/messages.seen";
    private static final String DEST_PRESENCE  = "/topic/presence";

    // Reconnect backoff: 2s, 4s, 8s, 16s, 32s (capped)
    private static final long[] BACKOFF_SECONDS = {2, 4, 8, 16, 32};

    private static volatile StompManager instance;

    private final Context appContext;
    private final Gson gson = new Gson();

    private StompClient stompClient;
    private CompositeDisposable disposables = new CompositeDisposable();

    /** Broadcasts all socket events to subscribers. */
    private final PublishSubject<SocketEvent> eventSubject = PublishSubject.create();

    /** Group-chat topic subscriptions keyed by conversationId. */
    private final Map<String, Disposable> groupSubs = new HashMap<>();

    private String currentToken;
    private int reconnectAttempt = 0;
    private boolean intentionalDisconnect = false;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private StompManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static StompManager getInstance(Context context) {
        if (instance == null) {
            synchronized (StompManager.class) {
                if (instance == null) {
                    instance = new StompManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Connect to the STOMP endpoint with the given Bearer token.
     * Safe to call multiple times — will disconnect the old client first.
     */
    public void connect(String token) {
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "connect() called with null/empty token — skipping");
            return;
        }
        this.currentToken = token;
        this.intentionalDisconnect = false;
        this.reconnectAttempt = 0;
        doConnect(token);
    }

    /** Disconnect cleanly (e.g. on logout). Will not attempt reconnect. */
    public void disconnect() {
        intentionalDisconnect = true;
        disposeAll();
        if (stompClient != null) {
            try { stompClient.disconnect(); } catch (Exception ignored) {}
            stompClient = null;
        }
        Log.d(TAG, "Disconnected intentionally");
    }

    /**
     * Observable stream of all socket events (MESSAGE_RECEIVED, SENT, DELIVERED, SEEN).
     * Emits on the main thread.
     */
    public Observable<SocketEvent> events() {
        return eventSubject.observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Subscribe to a group conversation topic.
     * Call this when ChatFragment opens with isGroup=true.
     */
    public void subscribeGroup(String conversationId) {
        if (stompClient == null || !stompClient.isConnected()) {
            Log.w(TAG, "subscribeGroup: not connected yet, will retry after connect");
            return;
        }
        if (groupSubs.containsKey(conversationId)) return; // already subscribed

        String dest = "/topic/messages.receive-" + conversationId;
        Disposable d = stompClient.topic(dest)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        msg -> handlePayload(msg.getPayload(), SocketEvent.Type.MESSAGE_RECEIVED),
                        err -> Log.e(TAG, "Group topic error: " + dest, err)
                );
        groupSubs.put(conversationId, d);
        disposables.add(d);
        Log.d(TAG, "Subscribed to group topic: " + dest);
    }

    /** Unsubscribe from a group conversation topic. */
    public void unsubscribeGroup(String conversationId) {
        Disposable d = groupSubs.remove(conversationId);
        if (d != null && !d.isDisposed()) d.dispose();
    }

    /**
     * Send a call signal payload to /app/calls.signal.
     * Safe to call even if not yet connected — will log a warning.
     */
    public void sendCallSignal(String jsonPayload) {
        if (stompClient != null && stompClient.isConnected()) {
            stompClient.send(CallSignalingManager.DEST_PUBLISH, jsonPayload)
                    .subscribe(
                            () -> Log.d(TAG, "Call signal sent"),
                            err -> Log.e(TAG, "Failed to send call signal", err)
                    );
        } else {
            Log.e(TAG, "sendCallSignal: StompClient not connected");
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void doConnect(String token) {
        // Clean up previous client
        disposeAll();
        if (stompClient != null) {
            try { stompClient.disconnect(); } catch (Exception ignored) {}
        }

        // Pass Authorization header via connectHttpHeaders (3rd param of Stomp.over)
        // StompProtocolAndroid 1.6.6: headers in connect() are NOT sent in CONNECT frame;
        // they must be passed as HTTP upgrade headers in Stomp.over().
        java.util.Map<String, String> httpHeaders = new java.util.HashMap<>();
        httpHeaders.put("Authorization", "Bearer " + token);

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP,
                AppConfig.WEBSOCKET_URL, httpHeaders);
        stompClient.withClientHeartbeat(10_000).withServerHeartbeat(10_000);

        disposables = new CompositeDisposable();

        // Also build STOMP-level connect headers as fallback
        List<StompHeader> stompHeaders = new ArrayList<>();
        stompHeaders.add(new StompHeader("Authorization", "Bearer " + token));
        stompHeaders.add(new StompHeader(StompHeader.VERSION, "1.1,1.2"));
        stompHeaders.add(new StompHeader(StompHeader.HEART_BEAT, "10000,10000"));

        // Lifecycle
        Disposable lifecycle = stompClient.lifecycle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    switch (event.getType()) {
                        case OPENED:
                            Log.d(TAG, "STOMP connected");
                            reconnectAttempt = 0;
                            subscribeUserQueues();
                            break;
                        case CLOSED:
                            Log.w(TAG, "STOMP closed");
                            if (!intentionalDisconnect) scheduleReconnect();
                            break;
                        case ERROR:
                            Log.e(TAG, "STOMP error: " + event.getException());
                            if (!intentionalDisconnect) scheduleReconnect();
                            break;
                        default:
                            break;
                    }
                }, err -> Log.e(TAG, "Lifecycle error", err));

        disposables.add(lifecycle);
        stompClient.connect(stompHeaders);
    }

    private void subscribeUserQueues() {
        subscribe(DEST_RECEIVE,   SocketEvent.Type.MESSAGE_RECEIVED);
        subscribe(DEST_SENT,      SocketEvent.Type.MESSAGE_SENT);
        subscribe(DEST_DELIVERED, SocketEvent.Type.MESSAGE_DELIVERED);
        subscribe(DEST_SEEN,      SocketEvent.Type.MESSAGE_SEEN);

        // Call signals
        CallSignalingManager.getInstance(appContext).subscribeCallSignals(stompClient);

        // Presence
        subscribePresence();
    }

    private void subscribePresence() {
        Disposable d = stompClient.topic(DEST_PRESENCE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        msg -> {
                            try {
                                PresenceUpdate presenceUpdate = gson.fromJson(msg.getPayload(), PresenceUpdate.class);
                                if (presenceUpdate != null) {
                                    eventSubject.onNext(new SocketEvent(SocketEvent.Type.PRESENCE_UPDATE, presenceUpdate));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse presence payload", e);
                            }
                        },
                        err -> Log.e(TAG, "Error subscribing to presence", err)
                );
        disposables.add(d);
    }

    private void subscribe(String destination, SocketEvent.Type type) {
        Disposable d = stompClient.topic(destination)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        msg -> handlePayload(msg.getPayload(), type),
                        err -> Log.e(TAG, "Subscription error: " + destination, err)
                );
        disposables.add(d);
        Log.d(TAG, "Subscribed: " + destination);
    }

    private void handlePayload(String payload, SocketEvent.Type type) {
        try {
            ChatMessage msg = gson.fromJson(payload, ChatMessage.class);
            if (msg != null) {
                eventSubject.onNext(new SocketEvent(type, msg));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse payload: " + payload, e);
        }
    }

    private void scheduleReconnect() {
        long delaySec = BACKOFF_SECONDS[Math.min(reconnectAttempt, BACKOFF_SECONDS.length - 1)];
        reconnectAttempt++;
        Log.d(TAG, "Reconnecting in " + delaySec + "s (attempt " + reconnectAttempt + ")");

        Disposable d = Observable.timer(delaySec, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tick -> {
                    if (!intentionalDisconnect && currentToken != null) {
                        doConnect(currentToken);
                    }
                }, err -> Log.e(TAG, "Reconnect timer error", err));
        disposables.add(d);
    }

    private void disposeAll() {
        if (disposables != null && !disposables.isDisposed()) {
            disposables.dispose();
        }
        for (Disposable d : groupSubs.values()) {
            if (d != null && !d.isDisposed()) d.dispose();
        }
        groupSubs.clear();
    }
}
