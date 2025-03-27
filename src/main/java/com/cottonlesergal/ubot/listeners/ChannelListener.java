package com.cottonlesergal.ubot.listeners;

import com.cottonlesergal.ubot.dtos.ChannelDTO;
import com.cottonlesergal.ubot.websocket.WebSocketEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateParentEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateTopicEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for Discord channel events.
 * Processes channel creation, deletion, and update events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelListener extends ListenerAdapter {

    private final WebSocketEventHandler eventHandler;

    /**
     * Handle channel creation events
     */
    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        GuildChannel channel = (GuildChannel) event.getChannel();
        String guildId = event.getGuild().getId();

        log.debug("Channel created: {} ({}), type: {}, guild: {}",
                channel.getName(), channel.getId(), channel.getType(), guildId);

        // Convert to DTO
        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.setId(channel.getId());
        channelDTO.setName(channel.getName());
        channelDTO.setType(channel.getType().toString());
        channelDTO.setGuildId(guildId);

        // If channel has a parent category, set it
        if (channel instanceof net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel categorizableChannel) {
            if (categorizableChannel.getParentCategory() != null) {
                channelDTO.setCategoryId(categorizableChannel.getParentCategory().getId());
            }
        }

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("channel", channelDTO);
        eventData.put("guildId", guildId);

        // Broadcast to clients subscribed to this guild
        eventHandler.broadcastToGuild(guildId, "CHANNEL_CREATE", eventData);
    }

    /**
     * Handle channel deletion events
     */
    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        GuildChannel channel = (GuildChannel) event.getChannel();
        String guildId = event.getGuild().getId();

        log.debug("Channel deleted: {} ({}), type: {}, guild: {}",
                channel.getName(), channel.getId(), channel.getType(), guildId);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("channelId", channel.getId());
        eventData.put("guildId", guildId);

        // Broadcast to clients subscribed to this guild
        eventHandler.broadcastToGuild(guildId, "CHANNEL_DELETE", eventData);
    }

    /**
     * Handle channel name update events
     */
    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        GuildChannel channel = (GuildChannel) event.getChannel();
        String guildId = event.getGuild().getId();
        String oldName = event.getOldValue();
        String newName = event.getNewValue();

        log.debug("Channel name updated: {} -> {} ({}), guild: {}",
                oldName, newName, channel.getId(), guildId);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("channelId", channel.getId());
        eventData.put("guildId", guildId);
        eventData.put("oldName", oldName);
        eventData.put("newName", newName);

        // Broadcast to clients subscribed to this guild and specific channel
        eventHandler.broadcastToGuild(guildId, "CHANNEL_UPDATE_NAME", eventData);
        eventHandler.broadcastToChannel(channel.getId(), "CHANNEL_UPDATE", eventData);
    }

    /**
     * Handle channel topic update events
     */
    @Override
    public void onChannelUpdateTopic(ChannelUpdateTopicEvent event) {
        GuildChannel channel = (GuildChannel) event.getChannel();
        String guildId = event.getGuild().getId();
        String oldTopic = event.getOldValue();
        String newTopic = event.getNewValue();

        log.debug("Channel topic updated: {} ({}), guild: {}",
                channel.getName(), channel.getId(), guildId);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("channelId", channel.getId());
        eventData.put("guildId", guildId);
        eventData.put("oldTopic", oldTopic);
        eventData.put("newTopic", newTopic);

        // Broadcast to clients subscribed to this guild and specific channel
        eventHandler.broadcastToGuild(guildId, "CHANNEL_UPDATE_TOPIC", eventData);
        eventHandler.broadcastToChannel(channel.getId(), "CHANNEL_UPDATE", eventData);
    }

    /**
     * Handle channel parent update events (moving to different category)
     */
    @Override
    public void onChannelUpdateParent(ChannelUpdateParentEvent event) {
        GuildChannel channel = (GuildChannel) event.getChannel();
        String guildId = event.getGuild().getId();
        String oldParentId = event.getOldValue() != null ? event.getOldValue().getId() : null;
        String newParentId = event.getNewValue() != null ? event.getNewValue().getId() : null;

        log.debug("Channel category updated: {} ({}), guild: {}, category: {} -> {}",
                channel.getName(), channel.getId(), guildId, oldParentId, newParentId);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("channelId", channel.getId());
        eventData.put("guildId", guildId);
        eventData.put("oldCategoryId", oldParentId);
        eventData.put("newCategoryId", newParentId);

        // Broadcast to clients subscribed to this guild and specific channel
        eventHandler.broadcastToGuild(guildId, "CHANNEL_UPDATE_CATEGORY", eventData);
        eventHandler.broadcastToChannel(channel.getId(), "CHANNEL_UPDATE", eventData);
    }
}