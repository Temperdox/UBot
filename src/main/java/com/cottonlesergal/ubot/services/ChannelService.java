package com.cottonlesergal.ubot.services;

import com.cottonlesergal.ubot.dtos.ChannelDTO;
import com.cottonlesergal.ubot.exceptions.DiscordApiException;
import com.cottonlesergal.ubot.providers.JDAProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing Discord channels
 */
@Service
@Slf4j
public class ChannelService {

    private final JDAProvider jda;

    public ChannelService(@Lazy JDAProvider jda) {
        this.jda = jda;
    }

    /**
     * Get all channels in a guild
     *
     * @param guildId The ID of the guild
     * @return List of channel DTOs
     */
    public List<ChannelDTO> getChannels(String guildId) {
        Guild guild = jda.getJda().getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.getChannels().stream()
                .filter(channel -> channel instanceof GuildChannel)
                .map(channel -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(channel.getId());
                    dto.setName(channel.getName());
                    dto.setType(channel.getType().toString());
                    dto.setGuildId(guild.getId());
                    if (channel instanceof ICategorizableChannel categorizableChannel &&
                            categorizableChannel.getParentCategory() != null) {
                        dto.setCategoryId(categorizableChannel.getParentCategory().getId());
                    }
                    return dto;
                })
                .toList();
    }

    /**
     * Get a channel by ID
     *
     * @param channelId The ID of the channel
     * @return Channel DTO
     */
    public ChannelDTO getChannel(String channelId) {
        GuildChannel channel = jda.getJda().getChannelById(GuildChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        ChannelDTO dto = new ChannelDTO();
        dto.setId(channel.getId());
        dto.setName(channel.getName());
        dto.setType(channel.getType().toString());
        dto.setGuildId(channel.getGuild().getId());
        if (channel instanceof ICategorizableChannel categorizableChannel &&
                categorizableChannel.getParentCategory() != null) {
            dto.setCategoryId(categorizableChannel.getParentCategory().getId());
        }
        return dto;
    }

    /**
     * Get text channels in a guild
     *
     * @param guildId The ID of the guild
     * @return List of text channel DTOs
     */
    public List<ChannelDTO> getTextChannels(String guildId) {
        Guild guild = jda.getJda().getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.getTextChannels().stream()
                .map(channel -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(channel.getId());
                    dto.setName(channel.getName());
                    dto.setType(channel.getType().toString());
                    dto.setGuildId(guild.getId());
                    return dto;
                })
                .toList();
    }

    /**
     * Create a new text channel in a guild
     *
     * @param guildId The ID of the guild
     * @param channelName The name of the new channel
     * @param categoryId The ID of the category to place the channel in (optional)
     * @return CompletableFuture with the created channel DTO
     */
    public CompletableFuture<ChannelDTO> createTextChannel(String guildId, String channelName, String categoryId) {
        Guild guild = jda.getJda().getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        // Create the channel action
        ChannelAction<TextChannel> channelAction =
                guild.createTextChannel(channelName);

        // Set category if provided
        if (categoryId != null && !categoryId.isEmpty()) {
            Category category =
                    guild.getCategoryById(categoryId);
            if (category == null) {
                throw new DiscordApiException("Category not found with ID: " + categoryId);
            }
            channelAction = channelAction.setParent(category);
        }

        return channelAction.submit()
                .thenApply(channel -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(channel.getId());
                    dto.setName(channel.getName());
                    dto.setType(channel.getType().toString());
                    dto.setGuildId(guild.getId());
                    if (channel.getParentCategory() != null) {
                        dto.setCategoryId(channel.getParentCategory().getId());
                    }
                    return dto;
                })
                .exceptionally(ex -> {
                    log.error("Failed to create text channel", ex);
                    throw new DiscordApiException("Failed to create text channel: " + ex.getMessage());
                });
    }

    /**
     * Delete a channel
     *
     * @param channelId The ID of the channel to delete
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> deleteChannel(String channelId) {
        TextChannel channel = jda.getJda().getTextChannelById(channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.delete()
                .submit()
                .exceptionally(ex -> {
                    log.error("Failed to delete channel", ex);
                    throw new DiscordApiException("Failed to delete channel: " + ex.getMessage());
                });
    }

    /**
     * Update a channel's name
     *
     * @param channelId The ID of the channel to update
     * @param newName The new name for the channel
     * @return CompletableFuture with the updated channel DTO
     */
    public CompletableFuture<ChannelDTO> updateChannelName(String channelId, String newName) {
        TextChannel channel = jda.getJda().getTextChannelById(channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.getManager().setName(newName)
                .submit()
                .thenApply(unused -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(channel.getId());
                    dto.setName(newName);
                    dto.setType(channel.getType().toString());
                    dto.setGuildId(channel.getGuild().getId());
                    return dto;
                })
                .exceptionally(ex -> {
                    log.error("Failed to update channel name", ex);
                    throw new DiscordApiException("Failed to update channel name: " + ex.getMessage());
                });
    }
    /**
     * Create a new voice channel in a guild
     *
     * @param guildId The ID of the guild
     * @param channelName The name of the new channel
     * @param categoryId The ID of the category to place the channel in (optional)
     * @return CompletableFuture with the created channel DTO
     */
    public CompletableFuture<ChannelDTO> createVoiceChannel(String guildId, String channelName, String categoryId) {
        Guild guild = jda.getJda().getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        // Create the channel action
        ChannelAction<VoiceChannel> channelAction =
                guild.createVoiceChannel(channelName);

        // Set category if provided
        if (categoryId != null && !categoryId.isEmpty()) {
            Category category =
                    guild.getCategoryById(categoryId);
            if (category == null) {
                throw new DiscordApiException("Category not found with ID: " + categoryId);
            }
            channelAction = channelAction.setParent(category);
        }

        return channelAction.submit()
                .thenApply(channel -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(channel.getId());
                    dto.setName(channel.getName());
                    dto.setType(channel.getType().toString());
                    dto.setGuildId(guild.getId());
                    if (channel.getParentCategory() != null) {
                        dto.setCategoryId(channel.getParentCategory().getId());
                    }
                    return dto;
                })
                .exceptionally(ex -> {
                    log.error("Failed to create voice channel", ex);
                    throw new DiscordApiException("Failed to create voice channel: " + ex.getMessage());
                });
    }

    /**
     * Update a channel's topic (text channels only)
     *
     * @param channelId The ID of the channel
     * @param topic The new topic for the channel
     * @return CompletableFuture with the updated channel DTO
     */
    public CompletableFuture<ChannelDTO> updateChannelTopic(String channelId, String topic) {;
        TextChannel channel = jda.getJda().getTextChannelById(channelId);
        if (channel == null) {
            throw new DiscordApiException("Text channel not found with ID: " + channelId);
        }

        return channel.getManager().setTopic(topic)
                .submit()
                .thenApply(unused -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(channel.getId());
                    dto.setName(channel.getName());
                    dto.setType(channel.getType().toString());
                    dto.setGuildId(channel.getGuild().getId());
                    if (channel.getParentCategory() != null) {
                        dto.setCategoryId(channel.getParentCategory().getId());
                    }
                    return dto;
                })
                .exceptionally(ex -> {
                    log.error("Failed to update channel topic", ex);
                    throw new DiscordApiException("Failed to update channel topic: " + ex.getMessage());
                });
    }

    /**
     * Move a channel to a different category
     *
     * @param channelId The ID of the channel
     * @param categoryId The ID of the category to move the channel to
     * @return CompletableFuture with the updated channel DTO
     */
    public CompletableFuture<ChannelDTO> moveChannelToCategory(String channelId, String categoryId) {

        // Try to get the channel as a generic guild channel first
        GuildChannel channel =
                jda.getJda().getChannelById(GuildChannel.class, channelId);

        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        // Get the guild from the channel
        Guild guild = channel.getGuild();

        // Get the category
        Category category = guild.getCategoryById(categoryId);
        if (category == null) {
            throw new DiscordApiException("Category not found with ID: " + categoryId);
        }

        // Check if the channel can be categorized
        if (!(channel instanceof ICategorizableChannel)) {
            throw new DiscordApiException("Channel cannot be moved to a category: " + channelId);
        }

        return ((ICategorizableChannel) channel)
                .getManager().setParent(category)
                .submit()
                .thenApply(unused -> {
                    ChannelDTO dto = new ChannelDTO();
                    dto.setId(channel.getId());
                    dto.setName(channel.getName());
                    dto.setType(channel.getType().toString());
                    dto.setGuildId(guild.getId());
                    dto.setCategoryId(categoryId);
                    return dto;
                })
                .exceptionally(ex -> {
                    log.error("Failed to move channel to category", ex);
                    throw new DiscordApiException("Failed to move channel to category: " + ex.getMessage());
                });
    }

}