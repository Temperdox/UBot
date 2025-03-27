package com.cottonlesergal.ubot.listeners;

import com.cottonlesergal.ubot.dtos.UserDTO;
import com.cottonlesergal.ubot.websocket.WebSocketEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for Discord user events.
 * Processes user status changes, profile updates, and the bot's ready state.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserListener extends ListenerAdapter {

    private final WebSocketEventHandler eventHandler;

    /**
     * Handle JDA ready event (bot connected and ready)
     */
    @Override
    public void onReady(ReadyEvent event) {
        User selfUser = event.getJDA().getSelfUser();
        int guildCount = event.getGuildAvailableCount();
        int unavailableGuilds = event.getGuildUnavailableCount();

        log.info("Bot ready! Logged in as: {} ({})", selfUser.getName(), selfUser.getId());
        log.info("Connected to {} guilds ({} unavailable)", guildCount, unavailableGuilds);

        // Convert to DTO
        UserDTO userDTO = UserDTO.builder()
                .id(selfUser.getId())
                .name(selfUser.getName())
                .avatarUrl(selfUser.getEffectiveAvatarUrl())
                .bot(true)
                .build();

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("bot", userDTO);
        eventData.put("guildCount", guildCount);
        eventData.put("unavailableGuilds", unavailableGuilds);

        // Broadcast to all connected clients
        eventHandler.broadcastToAll("BOT_READY", eventData);
    }

    /**
     * Handle user name changes
     */
    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        User user = event.getUser();
        String oldName = event.getOldName();
        String newName = event.getNewName();

        log.debug("User name updated: {} -> {} ({})", oldName, newName, user.getId());

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", user.getId());
        eventData.put("oldName", oldName);
        eventData.put("newName", newName);

        // Broadcast to clients tracking this user
        eventHandler.broadcastToUserSubscribers(user.getId(), "USER_UPDATE_NAME", eventData);
    }

    /**
     * Handle user avatar changes
     */
    @Override
    public void onUserUpdateAvatar(UserUpdateAvatarEvent event) {
        User user = event.getUser();
        String oldAvatarUrl = event.getOldAvatarUrl();
        String newAvatarUrl = event.getNewAvatarUrl();

        log.debug("User avatar updated: {} ({})", user.getName(), user.getId());

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", user.getId());
        eventData.put("userName", user.getName());
        eventData.put("oldAvatarUrl", oldAvatarUrl);
        eventData.put("newAvatarUrl", newAvatarUrl);

        // Broadcast to clients tracking this user
        eventHandler.broadcastToUserSubscribers(user.getId(), "USER_UPDATE_AVATAR", eventData);
    }

    /**
     * Handle user status changes (online, offline, etc.)
     */
    @Override
    public void onUserUpdateOnlineStatus(UserUpdateOnlineStatusEvent event) {
        User user = event.getUser();
        String oldStatus = event.getOldOnlineStatus().toString();
        String newStatus = event.getNewOnlineStatus().toString();

        log.debug("User status updated: {} ({}): {} -> {}",
                user.getName(), user.getId(), oldStatus, newStatus);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", user.getId());
        eventData.put("userName", user.getName());
        eventData.put("oldStatus", oldStatus);
        eventData.put("newStatus", newStatus);
        eventData.put("guild", event.getGuild().getId());

        // Broadcast to clients tracking this user and guild
        eventHandler.broadcastToUserSubscribers(user.getId(), "USER_UPDATE_STATUS", eventData);
        eventHandler.broadcastToGuild(event.getGuild().getId(), "USER_UPDATE_STATUS", eventData);
    }

    /**
     * Handle user starting an activity
     */
    @Override
    public void onUserActivityStart(UserActivityStartEvent event) {
        User user = event.getUser();
        String activityName = event.getNewActivity().getName();

        log.debug("User started activity: {} ({}): {}",
                user.getName(), user.getId(), activityName);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", user.getId());
        eventData.put("userName", user.getName());
        eventData.put("activityName", activityName);
        eventData.put("activityType", event.getNewActivity().getType().toString());
        eventData.put("guild", event.getGuild().getId());

        // Broadcast to clients tracking this user and guild
        eventHandler.broadcastToUserSubscribers(user.getId(), "USER_ACTIVITY_START", eventData);
    }

    /**
     * Handle user ending an activity
     */
    @Override
    public void onUserActivityEnd(UserActivityEndEvent event) {
        User user = event.getUser();
        String activityName = event.getOldActivity().getName();

        log.debug("User ended activity: {} ({}): {}",
                user.getName(), user.getId(), activityName);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", user.getId());
        eventData.put("userName", user.getName());
        eventData.put("activityName", activityName);
        eventData.put("activityType", event.getOldActivity().getType().toString());
        eventData.put("guild", event.getGuild().getId());

        // Broadcast to clients tracking this user and guild
        eventHandler.broadcastToUserSubscribers(user.getId(), "USER_ACTIVITY_END", eventData);
    }
}