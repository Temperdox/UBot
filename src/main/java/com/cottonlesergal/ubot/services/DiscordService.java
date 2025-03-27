package com.cottonlesergal.ubot.services;

import com.cottonlesergal.ubot.dtos.ChannelDTO;
import com.cottonlesergal.ubot.dtos.GuildDTO;
import com.cottonlesergal.ubot.dtos.MessageDTO;
import com.cottonlesergal.ubot.dtos.UserDTO;
import com.cottonlesergal.ubot.exceptions.DiscordApiException;
import com.cottonlesergal.ubot.providers.JDAProvider;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for interacting with Discord API via JDA.
 */
@Service
@Slf4j
public class DiscordService {

    private final JDAProvider jdaProvider;

    @Autowired
    public DiscordService(JDAProvider jdaProvider) {
        this.jdaProvider = jdaProvider;
    }

    /**
     * Get the JDA instance
     */
    public JDA getJda() {
        return jdaProvider.getJda();
    }

    /**
     * Get all guilds (servers) the bot is connected to
     */
    public List<GuildDTO> getGuilds() {
        return getJda().getGuilds().stream()
                .map(this::convertToGuildDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a guild by ID
     */
    public GuildDTO getGuild(String guildId) {
        Guild guild = getJda().getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }
        return convertToGuildDTO(guild);
    }

    /**
     * Get all channels in a guild
     */
    public List<ChannelDTO> getChannels(String guildId) {
        Guild guild = getJda().getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.getChannels().stream()
                .filter(channel -> channel instanceof MessageChannel)
                .map(this::convertToChannelDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a channel by ID
     */
    public ChannelDTO getChannel(String channelId) {
        // First try to retrieve it as a channel
        Channel channel = getJda().getChannelById(Channel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return convertToChannelDTO(channel);
    }


    /**
     * Get messages from a channel
     */
    public CompletableFuture<List<MessageDTO>> getMessages(String channelId, int limit) {
        MessageChannel channel = getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.getHistory().retrievePast(limit)
                .submit()
                .thenApply(messages -> messages.stream()
                        .map(this::convertToMessageDTO)
                        .collect(Collectors.toList()));
    }

    /**
     * Send a message to a channel
     */
    public CompletableFuture<MessageDTO> sendMessage(String channelId, String content) {
        MessageChannel channel = getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        MessageCreateBuilder builder = new MessageCreateBuilder().setContent(content);

        return channel.sendMessage(builder.build())
                .submit()
                .thenApply(this::convertToMessageDTO)
                .exceptionally(ex -> {
                    log.error("Failed to send message", ex);
                    throw new DiscordApiException("Failed to send message: " + ex.getMessage());
                });
    }

    /**
     * Delete a message
     */
    public CompletableFuture<Void> deleteMessage(String channelId, String messageId) {
        MessageChannel channel = getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.retrieveMessageById(messageId)
                .submit()
                .thenCompose(message -> message.delete().submit())
                .exceptionally(ex -> {
                    log.error("Failed to delete message", ex);
                    throw new DiscordApiException("Failed to delete message: " + ex.getMessage());
                });
    }

    /**
     * Get members of a guild
     */
    public List<UserDTO> getGuildMembers(String guildId) {
        Guild guild = getJda().getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.getMembers().stream()
                .map(member -> convertToUserDTO(member.getUser(), member))
                .collect(Collectors.toList());
    }

    /**
     * Add a reaction to a message
     */
    public CompletableFuture<Void> addReaction(String channelId, String messageId, String emojiCode) {
        MessageChannel channel = getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        // Convert String emoji code to Emoji object
        Emoji emoji = Emoji.fromUnicode(emojiCode);

        return channel.retrieveMessageById(messageId)
                .submit()
                .thenCompose(message -> message.addReaction(emoji).submit())
                .exceptionally(ex -> {
                    log.error("Failed to add reaction", ex);
                    throw new DiscordApiException("Failed to add reaction: " + ex.getMessage());
                });
    }

    /**
     * Remove a reaction from a message
     */
    public CompletableFuture<Void> removeReaction(String channelId, String messageId, String emojiCode) {
        MessageChannel channel = getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        // Convert String emoji code to Emoji object
        Emoji emoji = Emoji.fromUnicode(emojiCode);

        return channel.retrieveMessageById(messageId)
                .submit()
                .thenCompose(message -> message.removeReaction(emoji).submit())
                .exceptionally(ex -> {
                    log.error("Failed to remove reaction", ex);
                    throw new DiscordApiException("Failed to remove reaction: " + ex.getMessage());
                });
    }


    /**
     * Edit a message
     */
    public CompletableFuture<MessageDTO> editMessage(String channelId, String messageId, String newContent) {
        MessageChannel channel = getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.retrieveMessageById(messageId)
                .submit()
                .thenCompose(message -> message.editMessage(newContent).submit())
                .thenApply(this::convertToMessageDTO)
                .exceptionally(ex -> {
                    log.error("Failed to edit message", ex);
                    throw new DiscordApiException("Failed to edit message: " + ex.getMessage());
                });
    }

    /**
     * Get user by ID
     */
    public UserDTO getUser(String userId) {
        User user = getJda().getUserById(userId);
        if (user == null) {
            try {
                user = getJda().retrieveUserById(userId).complete();
            } catch (Exception e) {
                throw new DiscordApiException("User not found with ID: " + userId);
            }
        }
        return convertToUserDTO(user);
    }

    // Converter methods

    private GuildDTO convertToGuildDTO(Guild guild) {
        return GuildDTO.builder()
                .id(guild.getId())
                .name(guild.getName())
                .iconUrl(guild.getIconUrl())
                .memberCount(guild.getMemberCount())
                .ownerId(guild.getOwnerId())
                .build();
    }

    /**
     * Convert a JDA Channel to a ChannelDTO
     * Handles both guild channels and private channels
     */
    private ChannelDTO convertToChannelDTO(Channel channel) {
        ChannelDTO dto = new ChannelDTO();
        dto.setId(channel.getId());

        // Handle different types of channels
        if (channel instanceof GuildChannel guildChannel) {
            dto.setName(guildChannel.getName());
            dto.setGuildId(guildChannel.getGuild().getId());
            dto.setType(channel.getType().toString());
        }
        else if (channel instanceof PrivateChannel privateChannel) {
            dto.setName(Objects.requireNonNull(privateChannel.getUser()).getName());
            dto.setType("DM");
            // No guild ID for DMs
        }

        // Add additional properties based on specific channel types
        if (channel instanceof ICategorizableChannel categorizableChannel && categorizableChannel.getParentCategory() != null) {
            dto.setCategoryId(categorizableChannel.getParentCategory().getId());
        }

        return dto;
    }


    private MessageDTO convertToMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .content(message.getContentRaw())
                .channelId(message.getChannel().getId())
                .authorId(message.getAuthor().getId())
                .authorName(message.getAuthor().getName())
                .authorAvatarUrl(message.getAuthor().getEffectiveAvatarUrl())
                .timestamp(message.getTimeCreated().toInstant().toEpochMilli())
                .build();
    }

    UserDTO convertToUserDTO(User user) {
        return convertToUserDTO(user, null);
    }

    /**
     * Convert a User and optional Member to UserDTO
     *
     * @param user The Discord User
     * @param member The Discord Member (can be null for non-guild contexts)
     * @return UserDTO representation of the user
     */
    UserDTO convertToUserDTO(User user, Member member) {
        UserDTO.UserDTOBuilder builder = UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .discriminator(user.getDiscriminator())
                .avatarUrl(user.getEffectiveAvatarUrl())
                .bot(user.isBot());

        // Add member-specific information if available
        if (member != null) {
            builder.nickname(member.getNickname())
                    .status(member.getOnlineStatus().toString())
                    .roles(member.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList()));
        }

        return builder.build();
    }
}