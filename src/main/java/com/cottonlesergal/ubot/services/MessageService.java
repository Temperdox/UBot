package com.cottonlesergal.ubot.services;

import com.cottonlesergal.ubot.dtos.MessageDTO;
import com.cottonlesergal.ubot.entities.Channel;
import com.cottonlesergal.ubot.entities.Guild;
import com.cottonlesergal.ubot.entities.Message;
import com.cottonlesergal.ubot.entities.MessageAttachment;
import com.cottonlesergal.ubot.exceptions.DiscordApiException;
import com.cottonlesergal.ubot.exceptions.ResourceNotFoundException;
import com.cottonlesergal.ubot.providers.JDAProvider;
import com.cottonlesergal.ubot.repositories.ChannelRepository;
import com.cottonlesergal.ubot.repositories.GuildRepository;
import com.cottonlesergal.ubot.repositories.MessageAttachmentRepository;
import com.cottonlesergal.ubot.repositories.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing Discord messages.
 * Provides functionality for sending, retrieving, and manipulating messages.
 */
@Service
@Slf4j
public class MessageService {

    private final JDAProvider jda;
    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;

    @Autowired
    public MessageService(@Lazy JDAProvider jda, MessageRepository messageRepository, ChannelRepository channelRepository, MessageAttachmentRepository messageAttachmentRepository) {
        this.jda = jda;
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
    }

    private Channel fetchChannelFromJdaAndPersist(String channelId) {
        MessageChannel jdaChannel = jda.getJda().getChannelById(MessageChannel.class, channelId);
        if (jdaChannel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        Channel channel = new Channel();
        channel.setId(jdaChannel.getId());
        channel.setName(jdaChannel.getName());
        channel.setType(jdaChannel.getType().name());

        return channelRepository.save(channel);
    }

    private MessageDTO fetchMessageFromJdaAndPersist(String channelId, String messageId) {
        if (channelId == null) {
            throw new DiscordApiException("ChannelId must be provided when fetching a message not in DB.");
        }

        MessageChannel channel = jda.getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found for fetching message with ID: " + messageId);
        }

        try {
            net.dv8tion.jda.api.entities.Message jdaMessage = channel.retrieveMessageById(messageId).complete();
            saveMessageToDatabase(jdaMessage);
            return convertToJdaMessageDTO(jdaMessage);
        } catch (Exception e) {
            throw new DiscordApiException("Failed to retrieve message from Discord: " + e.getMessage());
        }
    }

    /**
     * Get a message by ID
     *
     * @param messageId The ID of the message
     * @return The message DTO
     * @throws ResourceNotFoundException if message not found
     */
    public MessageDTO getMessage(String messageId) {
        return messageRepository.findById(messageId)
                .map(this::convertToMessageDTO)
                .orElseGet(() -> fetchMessageFromJdaAndPersist(null, messageId));
    }

    /**
     * Get messages for a channel with pagination
     *
     * @param channelId The channel ID
     * @param pageable Pagination information
     * @return Page of message DTOs
     */
    public Page<MessageDTO> getMessagesForChannel(String channelId, Pageable pageable) {
        log.debug("Getting messages for channel ID: {}", channelId);

        try {
            // First check if channel exists
            Channel channel = channelRepository.findById(channelId)
                    .orElseGet(() -> {
                        log.warn("Channel not found in database, fetching from Discord API: {}", channelId);
                        return fetchChannelFromJdaAndPersist(channelId);
                    });

            // Get messages from repository
            Page<Message> messagePage = messageRepository.findByChannelId(channelId, pageable);

            // Log message count for debugging
            log.info("Found {} messages for channel {} in database",
                    messagePage.getContent().size(), channelId);

            // If no messages found, try to fetch from Discord API
            if (messagePage.getContent().isEmpty()) {
                log.info("No messages found in database, attempting to fetch from Discord API");

                // Fetch messages from Discord and save to database
                try {
                    List<net.dv8tion.jda.api.entities.Message> jdaMessages =
                            Objects.requireNonNull(jda.getJda()
                                            .getChannelById(MessageChannel.class, channelId))
                                    .getHistory()
                                    .retrievePast(pageable.getPageSize())
                                    .complete();

                    log.info("Fetched {} messages from Discord API", jdaMessages.size());

                    // Save messages to database
                    jdaMessages.forEach(this::saveMessageToDatabase);

                    // Retry database query
                    messagePage = messageRepository.findByChannelId(channelId, pageable);
                    log.info("After fetching from API, found {} messages in database",
                            messagePage.getContent().size());
                } catch (Exception e) {
                    log.error("Error fetching messages from Discord API: {}", e.getMessage(), e);
                }
            }

            return messagePage.map(this::convertToMessageDTO);
        } catch (Exception e) {
            log.error("Error retrieving messages for channel {}: {}", channelId, e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    /**
     * Get messages before a specific timestamp in a channel
     *
     * @param channelId The channel ID
     * @param timestamp The timestamp in milliseconds
     * @param limit Maximum number of messages to retrieve
     * @return List of message DTOs
     */
    public List<MessageDTO> getMessagesBeforeTimestamp(String channelId, long timestamp, int limit) {
        Channel channel = channelRepository.findById(channelId).orElseGet(() -> fetchChannelFromJdaAndPersist(channelId));

        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return messageRepository.findByChannelIdBeforeTimestamp(channelId, dateTime, PageRequest.of(0, limit))
                .stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search messages by content
     *
     * @param query The search query
     * @param pageable Pagination information
     * @return Page of message DTOs matching the query
     */
    public Page<MessageDTO> searchMessages(String query, Pageable pageable) {
        return messageRepository.searchByContent(query, pageable)
                .map(this::convertToMessageDTO);
    }

    /**
     * Send a message to a channel
     *
     * @param channelId The channel ID
     * @param content The message content
     * @return CompletableFuture with the sent message DTO
     */
    public CompletableFuture<MessageDTO> sendMessage(String channelId, String content) {
        MessageChannel channel = jda.getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.sendMessage(content)
                .submit()
                .thenApply(message -> {
                    // Convert to DTO
                    MessageDTO dto = convertToJdaMessageDTO(message);

                    // Save to database if needed
                    saveMessageToDatabase(message);

                    return dto;
                })
                .exceptionally(ex -> {
                    log.error("Failed to send message", ex);
                    throw new DiscordApiException("Failed to send message: " + ex.getMessage());
                });
    }

    /**
     * Send a message with file attachment
     *
     * @param channelId The channel ID
     * @param content The message content
     * @param file The file to upload
     * @return CompletableFuture with the sent message DTO
     */
    public CompletableFuture<MessageDTO> sendMessageWithFile(String channelId, String content, MultipartFile file) {
        MessageChannel channel = jda.getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        try {
            FileUpload fileUpload = FileUpload.fromData(file.getInputStream(), file.getOriginalFilename());

            return channel.sendMessage(content)
                    .addFiles(fileUpload)
                    .submit()
                    .thenApply(message -> {
                        // Convert to DTO
                        MessageDTO dto = convertToJdaMessageDTO(message);

                        // Save to database if needed
                        saveMessageToDatabase(message);

                        return dto;
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to send message with file", ex);
                        throw new DiscordApiException("Failed to send message with file: " + ex.getMessage());
                    });
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            throw new DiscordApiException("Failed to read uploaded file: " + e.getMessage());
        }
    }

    /**
     * Delete a message
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> deleteMessage(String channelId, String messageId) {
        MessageChannel channel = jda.getJda().getChannelById(MessageChannel.class, channelId);
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
     * Edit a message
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @param newContent The new content
     * @return CompletableFuture with the updated message DTO
     */
    public CompletableFuture<MessageDTO> editMessage(String channelId, String messageId, String newContent) {
        MessageChannel channel = jda.getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.retrieveMessageById(messageId)
                .submit()
                .thenCompose(message -> message.editMessage(newContent).submit())
                .thenApply(this::convertToJdaMessageDTO)
                .exceptionally(ex -> {
                    log.error("Failed to edit message", ex);
                    throw new DiscordApiException("Failed to edit message: " + ex.getMessage());
                });
    }

    /**
     * Pin a message to a channel
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> pinMessage(String channelId, String messageId) {
        TextChannel channel = jda.getJda().getTextChannelById(channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.retrieveMessageById(messageId)
                .submit()
                .thenCompose(message -> channel.pinMessageById(messageId).submit())
                .exceptionally(ex -> {
                    log.error("Failed to pin message", ex);
                    throw new DiscordApiException("Failed to pin message: " + ex.getMessage());
                });
    }

    /**
     * Unpin a message from a channel
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> unpinMessage(String channelId, String messageId) {
        TextChannel channel = jda.getJda().getTextChannelById(channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.retrieveMessageById(messageId)
                .submit()
                .thenCompose(message -> channel.unpinMessageById(messageId).submit())
                .exceptionally(ex -> {
                    log.error("Failed to unpin message", ex);
                    throw new DiscordApiException("Failed to unpin message: " + ex.getMessage());
                });
    }

    /**
     * Add a reaction to a message
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @param emojiCode The emoji code
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> addReaction(String channelId, String messageId, String emojiCode) {
        MessageChannel channel = jda.getJda().getChannelById(MessageChannel.class, channelId);
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
     *
     * @param channelId The channel ID
     * @param messageId The message ID
     * @param emojiCode The emoji code
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> removeReaction(String channelId, String messageId, String emojiCode) {
        MessageChannel channel = jda.getJda().getChannelById(MessageChannel.class, channelId);
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
     * Reply to a message
     *
     * @param channelId The channel ID
     * @param messageId The message ID to reply to
     * @param content The reply content
     * @return CompletableFuture with the sent message DTO
     */
    public CompletableFuture<MessageDTO> replyToMessage(String channelId, String messageId, String content) {
        MessageChannel channel = jda.getJda().getChannelById(MessageChannel.class, channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.retrieveMessageById(messageId)
                .submit()
                .thenCompose(message -> message.reply(content).submit())
                .thenApply(message -> {
                    // Convert to DTO
                    MessageDTO dto = convertToJdaMessageDTO(message);

                    // Save to database if needed
                    saveMessageToDatabase(message);

                    return dto;
                })
                .exceptionally(ex -> {
                    log.error("Failed to reply to message", ex);
                    throw new DiscordApiException("Failed to reply to message: " + ex.getMessage());
                });
    }

    /**
     * Get all pinned messages in a channel
     *
     * @param channelId The channel ID
     * @return CompletableFuture with list of pinned message DTOs
     */
    public CompletableFuture<List<MessageDTO>> getPinnedMessages(String channelId) {
        TextChannel channel = jda.getJda().getTextChannelById(channelId);
        if (channel == null) {
            throw new DiscordApiException("Channel not found with ID: " + channelId);
        }

        return channel.retrievePinnedMessages()
                .submit()
                .thenApply(messages -> messages.stream()
                        .map(this::convertToJdaMessageDTO)
                        .collect(Collectors.toList()))
                .exceptionally(ex -> {
                    log.error("Failed to get pinned messages", ex);
                    throw new DiscordApiException("Failed to get pinned messages: " + ex.getMessage());
                });
    }

    /**
     * Get message statistics for a channel
     *
     * @param channelId The channel ID
     * @return Map of statistics
     */
    public Map<String, Object> getMessageStats(String channelId) {
        // Verify channel exists
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", channelId));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", messageRepository.countByChannelId(channelId));

        // Add more statistics as needed
        // Example: Most active users, message frequency over time, etc.

        return stats;
    }

    /**
     * Save a JDA message to the database
     *
     * @param jdaMessage The JDA message to save
     */
    private void saveMessageToDatabase(net.dv8tion.jda.api.entities.Message jdaMessage) {
        try {
            log.debug("Saving message to database: {}", jdaMessage.getId());

            // Check if channel exists in DB
            String channelId = jdaMessage.getChannel().getId();
            Channel channel = channelRepository.findById(channelId)
                    .orElseGet(() -> {
                        // Create channel record if not exists
                        log.debug("Creating new channel record: {}", channelId);
                        Channel newChannel = new Channel();
                        newChannel.setId(channelId);
                        newChannel.setName(jdaMessage.getChannel().getName());
                        newChannel.setType(jdaMessage.getChannel().getType().toString());

                        return channelRepository.save(newChannel);
                    });

            // Create or update message record
            Message message = messageRepository.findById(jdaMessage.getId())
                    .orElse(new Message());

            message.setId(jdaMessage.getId());
            message.setContent(jdaMessage.getContentRaw());
            message.setChannel(channel);
            message.setAuthorId(jdaMessage.getAuthor().getId());
            message.setAuthorName(jdaMessage.getAuthor().getName());
            message.setTimestamp(LocalDateTime.ofInstant(
                    jdaMessage.getTimeCreated().toInstant(),
                    ZoneId.systemDefault()));

            message.setEdited(jdaMessage.isEdited());
            if (jdaMessage.isEdited() && jdaMessage.getTimeEdited() != null) {
                message.setEditedTimestamp(LocalDateTime.ofInstant(
                        jdaMessage.getTimeEdited().toInstant(),
                        ZoneId.systemDefault()));
            }

            // Set reference message ID if it's a reply
            if (jdaMessage.getReferencedMessage() != null) {
                message.setReferencedMessageId(jdaMessage.getReferencedMessage().getId());
            }

            // Save the message
            Message savedMessage = messageRepository.save(message);
            log.debug("Message saved successfully: {}", savedMessage.getId());

            // Process attachments if present
            if (!jdaMessage.getAttachments().isEmpty()) {
                processAttachments(jdaMessage, savedMessage);
            }

        } catch (Exception e) {
            log.error("Failed to save message to database: {}", e.getMessage(), e);
        }
    }

    /**
     * Process and save message attachments
     */
    private void processAttachments(net.dv8tion.jda.api.entities.Message jdaMessage, Message savedMessage) {
        jdaMessage.getAttachments().forEach(attachment -> {
            try {
                MessageAttachment attachmentEntity = new MessageAttachment();
                attachmentEntity.setId(attachment.getId());
                attachmentEntity.setMessage(savedMessage);
                attachmentEntity.setFileName(attachment.getFileName());
                attachmentEntity.setUrl(attachment.getUrl());
                attachmentEntity.setSize((long) attachment.getSize());

                if (attachment.getWidth() > 0) {
                    attachmentEntity.setWidth(attachment.getWidth());
                    attachmentEntity.setHeight(attachment.getHeight());
                }

                messageAttachmentRepository.save(attachmentEntity);
            } catch (Exception e) {
                log.error("Error saving attachment: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Convert a database Message entity to MessageDTO
     *
     * @param message The database Message entity
     * @return MessageDTO representation
     */
    private MessageDTO convertToMessageDTO(Message message) {
        MessageDTO.MessageDTOBuilder builder = MessageDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .channelId(message.getChannel().getId())
                .authorId(message.getAuthorId())
                .authorName(message.getAuthorName())
                .timestamp(message.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .edited(message.getEdited())
                .referencedMessageId(message.getReferencedMessageId());

        if (message.getEditedTimestamp() != null) {
            builder.editedTimestamp(message.getEditedTimestamp()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }

        // Add attachments, embeds, reactions if needed
        // This would typically involve mapping from the entity sets to DTO lists

        return builder.build();
    }

    /**
     * Convert a JDA Message to MessageDTO
     *
     * @param message The JDA Message
     * @return MessageDTO representation
     */
    private MessageDTO convertToJdaMessageDTO(net.dv8tion.jda.api.entities.Message message) {
        MessageDTO.MessageDTOBuilder builder = MessageDTO.builder()
                .id(message.getId())
                .content(message.getContentRaw())
                .channelId(message.getChannel().getId())
                .authorId(message.getAuthor().getId())
                .authorName(message.getAuthor().getName())
                .authorAvatarUrl(message.getAuthor().getEffectiveAvatarUrl())
                .timestamp(message.getTimeCreated().toInstant().toEpochMilli())
                .edited(message.isEdited());

        if (message.isEdited() && message.getTimeEdited() != null) {
            builder.editedTimestamp(message.getTimeEdited().toInstant().toEpochMilli());
        }

        // Add referenced message ID if it's a reply
        if (message.getReferencedMessage() != null) {
            builder.referencedMessageId(message.getReferencedMessage().getId());
        }

        // Add attachments if present
        if (!message.getAttachments().isEmpty()) {
            List<MessageDTO.AttachmentDTO> attachmentDTOs = message.getAttachments().stream()
                    .map(this::convertJdaAttachmentToDTO)
                    .collect(Collectors.toList());
            builder.attachments(attachmentDTOs);
        }

        // Add embeds if present
        if (!message.getEmbeds().isEmpty()) {
            List<MessageDTO.EmbedDTO> embedDTOs = message.getEmbeds().stream()
                    .map(this::convertJdaEmbedToDTO)
                    .collect(Collectors.toList());
            builder.embeds(embedDTOs);
        }

        // Add reactions if present
        if (!message.getReactions().isEmpty()) {
            Map<String, MessageDTO.ReactionDTO> reactionDTOs = new HashMap<>();
            message.getReactions().forEach(reaction -> {
                String emoji = reaction.getEmoji().getAsReactionCode();
                reactionDTOs.put(emoji, MessageDTO.ReactionDTO.builder()
                        .emoji(emoji)
                        .count(reaction.getCount())
                        .selfReacted(reaction.isSelf())
                        .build());
            });
            builder.reactions(reactionDTOs);
        }

        return builder.build();
    }

    /**
     * Convert a JDA Attachment to an AttachmentDTO
     *
     * @param attachment The JDA Attachment
     * @return AttachmentDTO representation
     */
    private MessageDTO.AttachmentDTO convertJdaAttachmentToDTO(Attachment attachment) {
        return MessageDTO.AttachmentDTO.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .url(attachment.getUrl())
                .size((long) attachment.getSize())
                .width(attachment.getWidth() > 0 ? attachment.getWidth() : null)
                .height(attachment.getHeight() > 0 ? attachment.getHeight() : null)
                .build();
    }

    /**
     * Convert a JDA Embed to an EmbedDTO
     *
     * @param embed The JDA Embed
     * @return EmbedDTO representation
     */
    private MessageDTO.EmbedDTO convertJdaEmbedToDTO(net.dv8tion.jda.api.entities.MessageEmbed embed) {
        MessageDTO.EmbedDTO.EmbedDTOBuilder builder = MessageDTO.EmbedDTO.builder()
                .title(embed.getTitle())
                .description(embed.getDescription())
                .url(embed.getUrl());

        if (embed.getTimestamp() != null) {
            builder.timestamp(embed.getTimestamp().toInstant().toEpochMilli());
        }

        if (embed.getColor() != null) {
            builder.color(embed.getColor().getRGB());
        }

        // Add author info if present
        if (embed.getAuthor() != null) {
            builder.author(MessageDTO.EmbedAuthorDTO.builder()
                    .name(embed.getAuthor().getName())
                    .url(embed.getAuthor().getUrl())
                    .iconUrl(embed.getAuthor().getIconUrl())
                    .build());
        }

        // Add fields if present
        if (!embed.getFields().isEmpty()) {
            List<MessageDTO.EmbedFieldDTO> fieldDTOs = embed.getFields().stream()
                    .map(field -> MessageDTO.EmbedFieldDTO.builder()
                            .name(field.getName())
                            .value(field.getValue())
                            .inline(field.isInline())
                            .build())
                    .collect(Collectors.toList());
            builder.fields(fieldDTOs);
        }

        // Add image if present
        if (embed.getImage() != null) {
            builder.image(MessageDTO.EmbedMediaDTO.builder()
                    .url(embed.getImage().getUrl())
                    .width(embed.getImage().getWidth())
                    .height(embed.getImage().getHeight())
                    .build());
        }

        // Add thumbnail if present
        if (embed.getThumbnail() != null) {
            builder.thumbnail(MessageDTO.EmbedMediaDTO.builder()
                    .url(embed.getThumbnail().getUrl())
                    .width(embed.getThumbnail().getWidth())
                    .height(embed.getThumbnail().getHeight())
                    .build());
        }

        // Add footer if present
        if (embed.getFooter() != null) {
            builder.footer(MessageDTO.EmbedFooterDTO.builder()
                    .text(embed.getFooter().getText())
                    .iconUrl(embed.getFooter().getIconUrl())
                    .build());
        }

        return builder.build();
    }
}