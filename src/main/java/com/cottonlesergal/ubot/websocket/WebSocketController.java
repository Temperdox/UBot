package com.cottonlesergal.ubot.websocket;

import com.cottonlesergal.ubot.dtos.events.DiscordEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket controller for handling real-time Discord events.
 * Manages WebSocket subscriptions and message handling.
 */
@Controller
@Slf4j
public class WebSocketController {

    private final WebSocketEventHandler eventHandler;
    private final WebSocketSessionRegistry sessionRegistry;

    @Autowired
    public WebSocketController(
            WebSocketEventHandler eventHandler,
            WebSocketSessionRegistry sessionRegistry) {
        this.eventHandler = eventHandler;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Handle channel subscription requests from clients
     */
    @MessageMapping("/subscribe/channel")
    public void subscribeToChannel(@Payload Map<String, String> subscription, SimpMessageHeaderAccessor headerAccessor) {
        String channelId = subscription.get("channelId");
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser().getName();

        log.debug("WebSocket subscription request for channel {}, from user {}, session {}",
                channelId, username, sessionId);

        // Register subscription for session tracking
        sessionRegistry.addChannelSubscription(channelId, sessionId);

        // Send acknowledgment
        Map<String, Object> ack = Map.of(
                "type", "SubscriptionAck",
                "channelId", channelId,
                "status", "subscribed"
        );

        eventHandler.sendToUser(username, "/queue/notifications", new DiscordEventDTO() {
            {
                setType("SubscriptionAck");
            }

            private Map<String, Object> data = ack;

            public Map<String, Object> getData() {
                return data;
            }
        });
    }

    /**
     * Handle guild subscription requests from clients
     */
    @MessageMapping("/subscribe/guild")
    public void subscribeToGuild(@Payload Map<String, String> subscription, SimpMessageHeaderAccessor headerAccessor) {
        String guildId = subscription.get("guildId");
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser().getName();

        log.debug("WebSocket subscription request for guild {}, from user {}, session {}",
                guildId, username, sessionId);

        // Register subscription for session tracking
        sessionRegistry.addGuildSubscription(guildId, sessionId);

        // Send acknowledgment
        Map<String, Object> ack = Map.of(
                "type", "SubscriptionAck",
                "guildId", guildId,
                "status", "subscribed"
        );

        eventHandler.sendToUser(username, "/queue/notifications", new DiscordEventDTO() {
            {
                setType("SubscriptionAck");
            }

            private Map<String, Object> data = ack;

            public Map<String, Object> getData() {
                return data;
            }
        });
    }

    /**
     * Handle DM subscription requests from clients
     */
    @MessageMapping("/subscribe/dm")
    public void subscribeToDM(@Payload Map<String, String> subscription, SimpMessageHeaderAccessor headerAccessor) {
        String userId = subscription.get("userId");
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser().getName();

        log.debug("WebSocket subscription request for DMs with user {}, from user {}, session {}",
                userId, username, sessionId);

        // Register subscription for session tracking
        sessionRegistry.addDmSubscription(userId, sessionId);

        // Send acknowledgment
        Map<String, Object> ack = Map.of(
                "type", "SubscriptionAck",
                "userId", userId,
                "status", "subscribed"
        );

        eventHandler.sendToUser(username, "/queue/notifications", new DiscordEventDTO() {
            {
                setType("SubscriptionAck");
            }

            private Map<String, Object> data = ack;

            public Map<String, Object> getData() {
                return data;
            }
        });
    }

    /**
     * Handle ping messages to keep connection alive
     */
    @MessageMapping("/ping")
    public void handlePing(@Payload Map<String, Object> ping, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser().getName();

        // Send pong response
        Map<String, Object> pong = Map.of(
                "type", "pong",
                "timestamp", System.currentTimeMillis()
        );

        eventHandler.sendToUser(username, "/queue/pong", new DiscordEventDTO() {
            {
                setType("Pong");
            }

            private Map<String, Object> data = pong;

            public Map<String, Object> getData() {
                return data;
            }
        });
    }
}