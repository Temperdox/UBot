package com.cottonlesergal.ubot.websocket;

import com.cottonlesergal.ubot.services.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor for WebSocket handshake to handle token-based authentication
 */
@Slf4j
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final AuthService authService;

    public WebSocketAuthHandshakeInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String token = servletRequest.getServletRequest().getParameter("token");

            if (token != null && authService.validateToken(token)) {
                try {
                    // Get user info from token
                    Map<String, Object> userInfo = authService.getUserInfoFromToken(token);
                    String userId = (String) userInfo.get("id");
                    String username = (String) userInfo.get("username");

                    // Store in session attributes
                    attributes.put("authenticated", true);
                    attributes.put("userId", userId);
                    attributes.put("username", username);

                    log.debug("WebSocket handshake authenticated for user {} ({})", username, userId);
                } catch (Exception e) {
                    log.error("Error processing authentication token during handshake", e);
                    // Don't reject connection, just don't mark as authenticated
                }
            }
        }

        // Always allow connection - authentication status is stored in attributes
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No actions needed after handshake
    }
}