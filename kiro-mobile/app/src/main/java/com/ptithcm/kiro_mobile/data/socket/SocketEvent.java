package com.ptithcm.kiro_mobile.data.socket;

import com.ptithcm.kiro_mobile.data.model.chat.ChatMessage;
import com.ptithcm.kiro_mobile.data.model.user.PresenceUpdate;

/**
 * Wraps a realtime socket event with its type and payload.
 */
public class SocketEvent {

    public enum Type {
        MESSAGE_RECEIVED,
        MESSAGE_SENT,
        MESSAGE_DELIVERED,
        MESSAGE_SEEN,
        PRESENCE_UPDATE
    }

    public final Type type;
    public final ChatMessage message;
    public final PresenceUpdate presenceUpdate;

    public SocketEvent(Type type, ChatMessage message) {
        this.type    = type;
        this.message = message;
        this.presenceUpdate = null;
    }

    public SocketEvent(Type type, PresenceUpdate presenceUpdate) {
        this.type = type;
        this.message = null;
        this.presenceUpdate = presenceUpdate;
    }
}
