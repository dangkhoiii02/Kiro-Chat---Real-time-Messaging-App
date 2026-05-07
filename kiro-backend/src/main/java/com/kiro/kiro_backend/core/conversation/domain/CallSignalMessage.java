package com.kiro.kiro_backend.core.conversation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSignalMessage {
    private SignalType type;
    private String conversationId;
    private String targetUserId;
    private String callerId;
    private String callerName;
    private String callerAvatar;
    /** true = video call (cam bật), false = voice only */
    @JsonProperty("withVideo")
    private Boolean withVideo = Boolean.TRUE;

    public enum SignalType {
        OFFER,
        ANSWER,
        REJECT,
        END
    }
}
