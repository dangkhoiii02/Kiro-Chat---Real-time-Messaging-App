package com.kiro.kiro_backend.core.conversation.infrastructure.primary.resource;

import com.kiro.kiro_backend.common.websocket.infrastructure.WsPaths;
import com.kiro.kiro_backend.core.conversation.domain.CallSignalMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CallSignalingController {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Receives a call signal from the sender and forwards it to the target user.
     * The client publishes to /app/calls.signal and the backend routes to
     * /user/{targetUserId}/queue/calls.signal on the target side.
     */
    @MessageMapping(WsPaths.APP_CALL_SIGNAL)
    public void processCallSignal(CallSignalMessage signalMessage, Principal principal) {
        log.info("Received call signal type={} from={} to={}",
                signalMessage.getType(),
                principal != null ? principal.getName() : "unknown",
                signalMessage.getTargetUserId());

        if (signalMessage.getTargetUserId() == null) {
            log.warn("Missing targetUserId in call signal, dropping message");
            return;
        }

        // Stamp the actual caller ID from the authenticated principal so the
        // client cannot spoof it.
        if (principal != null) {
            signalMessage.setCallerId(principal.getName());
        }

        messagingTemplate.convertAndSendToUser(
                signalMessage.getTargetUserId(),
                WsPaths.INDIVIDUAL_CALL_SIGNAL,
                signalMessage
        );

        log.debug("Forwarded call signal {} to user {}", signalMessage.getType(), signalMessage.getTargetUserId());
    }
}
