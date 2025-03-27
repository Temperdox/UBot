package com.cottonlesergal.ubot.websocket;

import com.cottonlesergal.ubot.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling WebSocket authentication.
 * Manages token validation and session authentication for WebSocket connections.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthController {

    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * Handle authentication requests via WebSocket
     * This allows clients to authenticate after connection is established
     *
     * @param headerAccessor STOMP message headers
     * @param payload Request payload containing authentication token
     */
    @MessageMapping("/auth")
    public void authenticate(SimpMessageHeaderAccessor headerAccessor, @Payload Map<String, String> payload) {
        String token = payload.get("token");
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";

        log.debug("WebSocket authentication request from session {}, user {}", sessionId, username);

        Map<String, Object> response = new HashMap<>();

        if (token != null && authService.validateToken(token)) {
            // Token is valid, get user info
            try {
                Map<String, Object> userInfo = authService.getUserInfoFromToken(token);
                String userId = (String) userInfo.get("id");
                String authenticatedUsername = (String) userInfo.get("username");

                // Store user info in session attributes
                headerAccessor.getSessionAttributes().put("authenticated", true);
                headerAccessor.getSessionAttributes().put("userId", userId);
                headerAccessor.getSessionAttributes().put("username", authenticatedUsername);

                // Register user session
                sessionRegistry.registerUserSession(sessionId, userId, authenticatedUsername);

                // Send success response
                response.put("type", "AUTH_SUCCESS");
                response.put("userId", userId);
                response.put("username", authenticatedUsername);

                log.info("WebSocket authentication successful for user {} ({}), session {}",
                        authenticatedUsername, userId, sessionId);
            } catch (Exception e) {
                log.error("Error processing authentication token", e);
                response.put("type", "AUTH_ERROR");
                response.put("error", "Invalid token");
            }
        } else {
            // Invalid token
            response.put("type", "AUTH_ERROR");
            response.put("error", "Invalid token");
            log.warn("WebSocket authentication failed for session {}, invalid token", sessionId);
        }

        // Send response to this specific client
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/auth",
                response
        );
    }

    /**
     * Handle session disconnect via WebSocket
     * This is a fallback in case the SessionDisconnectEvent doesn't trigger
     *
     * @param headerAccessor STOMP message headers
     */
    @MessageMapping("/logout")
    public void logout(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        log.debug("WebSocket logout request from session {}", sessionId);

        // Clean up session
        sessionRegistry.removeAllSubscriptionsForSession(sessionId);

        // Send acknowledgment
        Map<String, Object> response = new HashMap<>();
        response.put("type", "LOGOUT_SUCCESS");

        messagingTemplate.convertAndSendToUser(
                headerAccessor.getUser().getName(),
                "/queue/auth",
                response
        );

        log.info("WebSocket session logged out: {}", sessionId);
    }

    /**
     * Verify authentication status
     * Clients can use this to check if their session is authenticated
     *
     * @param headerAccessor STOMP message headers
     * @return Authentication status
     */
    @MessageMapping("/verify")
    public void verifyAuthentication(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";

        boolean isAuthenticated = headerAccessor.getSessionAttributes() != null &&
                Boolean.TRUE.equals(headerAccessor.getSessionAttributes().get("authenticated"));

        Map<String, Object> response = new HashMap<>();
        response.put("type", "AUTH_STATUS");
        response.put("authenticated", isAuthenticated);

        if (isAuthenticated) {
            response.put("userId", headerAccessor.getSessionAttributes().get("userId"));
            response.put("username", headerAccessor.getSessionAttributes().get("username"));
        }

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/auth",
                response
        );

        log.debug("WebSocket authentication verification for session {}: {}",
                sessionId, isAuthenticated ? "authenticated" : "not authenticated");
    }

    /**
     * Helper method to register a user session from other components
     * This can be used when authentication happens during the handshake
     *
     * @param sessionId The WebSocket session ID
     * @param userId The authenticated user ID
     * @param username The authenticated username
     */
    public void registerAuthenticatedSession(String sessionId, String userId, String username) {
        sessionRegistry.registerUserSession(sessionId, userId, username);
        log.info("Registered authenticated WebSocket session: {} for user {} ({})",
                sessionId, username, userId);
    }
}