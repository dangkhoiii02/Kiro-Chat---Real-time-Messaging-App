package com.kiro.kiro_backend.core.conversation.infrastructure.primary.resource;

import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
@Slf4j
public class CallResource {

    @Value("${livekit.api-key}")
    private String livekitApiKey;

    @Value("${livekit.api-secret}")
    private String livekitApiSecret;

    @GetMapping("/{conversationId}/token")
    public ResponseEntity<Map<String, String>> generateToken(@PathVariable String conversationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = "anonymous";
        String userName = "User";

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            userId = jwt.getSubject();
            userName = jwt.getClaimAsString("preferred_username");
            if (userName == null) {
                userName = jwt.getClaimAsString("name");
            }
        } else if (authentication != null) {
            userId = authentication.getName();
            userName = authentication.getName();
        }

        try {
            AccessToken token = new AccessToken(livekitApiKey, livekitApiSecret);
            token.setName(userName);
            token.setIdentity(userId);
            token.addGrants(new RoomJoin(true), new RoomName(conversationId));
            
            String jwtToken = token.toJwt();
            
            log.info("Generated LiveKit token for user {} in conversation {}", userId, conversationId);
            
            return ResponseEntity.ok(Map.of("token", jwtToken));
        } catch (Exception e) {
            log.error("Failed to generate LiveKit token", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Exception: " + e.getMessage() + ", Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null")));
        }
    }
}
