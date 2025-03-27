package com.cottonlesergal.ubot.websocket;

import com.cottonlesergal.ubot.dtos.events.DiscordEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler for WebSocket events.
 * Manages sending events to clients through WebSocket connections.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * Broadcast an event to all connected clients.
     *
     * @param eventType The type of event
     * @param eventData The event data
     */
    public void broadcastToAll(String eventType, Map<String, Object> eventData) {
        DiscordEventDTO eventDTO = new DiscordEventDTO();
        eventDTO.setType(eventType);
        eventDTO.setData(eventData);

        log.debug("Broadcasting event to all clients: {}", eventType);
        messagingTemplate.convertAndSend("/topic/events", eventDTO);
    }

    /**
     * Broadcast an event to clients subscribed to a specific guild.
     *
     * @param guildId The ID of the guild
     * @param eventType The type of event
     * @param eventData The event data
     */
    public void broadcastToGuild(String guildId, String eventType, Map<String, Object> eventData) {
        DiscordEventDTO eventDTO = new DiscordEventDTO();
        eventDTO.setType(eventType);
        eventDTO.setData(eventData);

        log.debug("Broadcasting event to guild {}: {}", guildId, eventType);
        messagingTemplate.convertAndSend("/topic/guilds/" + guildId, eventDTO);

        // Also send to specific sessions subscribed to this guild
        sessionRegistry.getSessionsSubscribedToGuild(guildId).forEach(sessionId -> {
            String destination = "/queue/sessions/" + sessionId;
            messagingTemplate.convertAndSend(destination, eventDTO);
        });
    }

    /**
     * Broadcast an event to clients subscribed to a specific channel.
     *
     * @param channelId The ID of the channel
     * @param eventType The type of event
     * @param eventData The event data
     */
    public void broadcastToChannel(String channelId, String eventType, Map<String, Object> eventData) {
        DiscordEventDTO eventDTO = new DiscordEventDTO();
        eventDTO.setType(eventType);
        eventDTO.setData(eventData);

        log.debug("Broadcasting event to channel {}: {}", channelId, eventType);
        messagingTemplate.convertAndSend("/topic/channels/" + channelId, eventDTO);

        // Also send to specific sessions subscribed to this channel
        sessionRegistry.getSessionsSubscribedToChannel(channelId).forEach(sessionId -> {
            String destination = "/queue/sessions/" + sessionId;
            messagingTemplate.convertAndSend(destination, eventDTO);
        });
    }

    /**
     * Send an event to a specific user.
     *
     * @param username The username
     * @param destination The destination topic/queue
     * @param eventDTO The event DTO
     */
    public void sendToUser(String username, String destination, DiscordEventDTO eventDTO) {
        log.debug("Sending event to user {}: {}", username, eventDTO.getType());
        messagingTemplate.convertAndSendToUser(username, destination, eventDTO);
    }

    /**
     * Broadcast an event to clients subscribed to a specific user.
     *
     * @param userId The ID of the user
     * @param eventType The type of event
     * @param eventData The event data
     */
    public void broadcastToUserSubscribers(String userId, String eventType, Map<String, Object> eventData) {
        DiscordEventDTO eventDTO = new DiscordEventDTO();
        eventDTO.setType(eventType);
        eventDTO.setData(eventData);

        log.debug("Broadcasting event about user {}: {}", userId, eventType);
        messagingTemplate.convertAndSend("/topic/users/" + userId, eventDTO);

        // Also send to specific sessions subscribed to this user
        sessionRegistry.getSessionsSubscribedToUser(userId).forEach(sessionId -> {
            String destination = "/queue/sessions/" + sessionId;
            messagingTemplate.convertAndSend(destination, eventDTO);
        });
    }

    /**
     * Send a direct message event to a specific session.
     *
     * @param sessionId The session ID
     * @param eventType The type of event
     * @param eventData The event data
     */
    public void sendToSession(String sessionId, String eventType, Map<String, Object> eventData) {
        DiscordEventDTO eventDTO = new DiscordEventDTO();
        eventDTO.setType(eventType);
        eventDTO.setData(eventData);

        log.debug("Sending event to session {}: {}", sessionId, eventType);
        messagingTemplate.convertAndSend("/queue/sessions/" + sessionId, eventDTO);
    }
}