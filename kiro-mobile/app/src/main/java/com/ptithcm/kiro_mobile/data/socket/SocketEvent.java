package com.ptithcm.kiro_mobile.data.socket;

import com.ptithcm.kiro_mobile.data.model.chat.ChatMessage;

/**
 * Wraps a realtime socket event with its type and payload.
 */
public class SocketEvent {

    public enum Type {
        MESSAGE_RECEIVED,
        MESSAGE_SENT,
        MESSAGE_DELIVERED,
        MESSAGE_SEEN
    }

    public final Type type;
    public final ChatMessage message;

    public SocketEvent(Type type, ChatMessage message) {
        this.type    = type;
        this.message = message;
    }
}
