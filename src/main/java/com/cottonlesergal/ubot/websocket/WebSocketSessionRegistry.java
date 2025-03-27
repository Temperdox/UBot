package com.cottonlesergal.ubot.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Registry for WebSocket sessions and subscriptions.
 * Manages tracking which sessions are subscribed to which resources.
 */
@Component
@Slf4j
public class WebSocketSessionRegistry {

    // Maps to track subscriptions
    private final Map<String, Set<String>> channelSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> guildSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userSubscriptions = new ConcurrentHashMap<>();

    // Reverse maps for cleanup
    private final Map<String, Set<String>> sessionToChannels = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionToGuilds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionToUsers = new ConcurrentHashMap<>();

    /**
     * Add a channel subscription for a session.
     *
     * @param channelId The channel ID
     * @param sessionId The session ID
     */
    public void addChannelSubscription(String channelId, String sessionId) {
        log.debug("Adding channel subscription: session {} -> channel {}", sessionId, channelId);

        // Add to channel map
        channelSubscriptions.computeIfAbsent(channelId, k -> new CopyOnWriteArraySet<>()).add(sessionId);

        // Add to session map for cleanup
        sessionToChannels.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>()).add(channelId);
    }

    /**
     * Add a guild subscription for a session.
     *
     * @param guildId The guild ID
     * @param sessionId The session ID
     */
    public void addGuildSubscription(String guildId, String sessionId) {
        log.debug("Adding guild subscription: session {} -> guild {}", sessionId, guildId);

        // Add to guild map
        guildSubscriptions.computeIfAbsent(guildId, k -> new CopyOnWriteArraySet<>()).add(sessionId);

        // Add to session map for cleanup
        sessionToGuilds.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>()).add(guildId);
    }

    /**
     * Add a user/DM subscription for a session.
     *
     * @param userId The user ID
     * @param sessionId The session ID
     */
    public void addDmSubscription(String userId, String sessionId) {
        log.debug("Adding user/DM subscription: session {} -> user {}", sessionId, userId);

        // Add to user map
        userSubscriptions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(sessionId);

        // Add to session map for cleanup
        sessionToUsers.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>()).add(userId);
    }

    /**
     * Get sessions subscribed to a channel.
     *
     * @param channelId The channel ID
     * @return Set of session IDs
     */
    public Set<String> getSessionsSubscribedToChannel(String channelId) {
        return channelSubscriptions.getOrDefault(channelId, Set.of());
    }

    /**
     * Get sessions subscribed to a guild.
     *
     * @param guildId The guild ID
     * @return Set of session IDs
     */
    public Set<String> getSessionsSubscribedToGuild(String guildId) {
        return guildSubscriptions.getOrDefault(guildId, Set.of());
    }

    /**
     * Get sessions subscribed to a user.
     *
     * @param userId The user ID
     * @return Set of session IDs
     */
    public Set<String> getSessionsSubscribedToUser(String userId) {
        return userSubscriptions.getOrDefault(userId, Set.of());
    }

    /**
     * Remove all subscriptions for a session.
     *
     * @param sessionId The session ID
     */
    public void removeAllSubscriptionsForSession(String sessionId) {
        log.debug("Removing all subscriptions for session: {}", sessionId);

        // Remove channel subscriptions
        Set<String> channelIds = sessionToChannels.remove(sessionId);
        if (channelIds != null) {
            channelIds.forEach(channelId -> {
                Set<String> sessions = channelSubscriptions.get(channelId);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        channelSubscriptions.remove(channelId);
                    }
                }
            });
        }

        // Remove guild subscriptions
        Set<String> guildIds = sessionToGuilds.remove(sessionId);
        if (guildIds != null) {
            guildIds.forEach(guildId -> {
                Set<String> sessions = guildSubscriptions.get(guildId);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        guildSubscriptions.remove(guildId);
                    }
                }
            });
        }

        // Remove user subscriptions
        Set<String> userIds = sessionToUsers.remove(sessionId);
        if (userIds != null) {
            userIds.forEach(userId -> {
                Set<String> sessions = userSubscriptions.get(userId);
                if (sessions != null) {
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()) {
                        userSubscriptions.remove(userId);
                    }
                }
            });
        }
    }

    /**
     * Event listener for session disconnect events.
     * Automatically cleans up subscriptions when a WebSocket session disconnects.
     *
     * @param event The disconnect event
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket session disconnected: {}", sessionId);
        removeAllSubscriptionsForSession(sessionId);
    }
}