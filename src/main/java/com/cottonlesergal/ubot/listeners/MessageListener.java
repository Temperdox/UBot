package com.cottonlesergal.ubot.listeners;

import com.cottonlesergal.ubot.dtos.MessageDTO;
import com.cottonlesergal.ubot.services.MessageService;
import com.cottonlesergal.ubot.websocket.WebSocketEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Listener for Discord message events.
 * Processes message creation, deletion, updates, and reactions.
 */
@Component
@Slf4j
public class MessageListener extends ListenerAdapter {

    private final WebSocketEventHandler eventHandler;
    private final MessageService messageService;

    @Autowired
    public MessageListener(WebSocketEventHandler eventHandler, @Lazy MessageService messageService) {
        this.eventHandler = eventHandler;
        this.messageService = messageService;
    }

    /**
     * Handle message received events
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore bot messages to prevent potential loops
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String channelId = message.getChannel().getId();
        String content = message.getContentRaw();
        String authorName = event.getAuthor().getName();
        String guildId = event.isFromGuild() ? event.getGuild().getId() : null;

        log.debug("Message received from {} in channel {}: {}",
                authorName,
                message.getChannel().getName(),
                message.getContentDisplay());

        // Convert to DTO
        MessageDTO messageDTO = convertToMessageDTO(message);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("message", messageDTO);
        eventData.put("channelId", channelId);
        if (guildId != null) {
            eventData.put("guildId", guildId);
        }

        // Broadcast to subscribed clients
        eventHandler.broadcastToChannel(channelId, "MESSAGE_CREATE", eventData);
        if (guildId != null) {
            eventHandler.broadcastToGuild(guildId, "MESSAGE_CREATE", eventData);
        }

        // Process commands if needed
        if (content.startsWith("!")) {
            processCommand(message);
        }
    }

    /**
     * Handle message update events
     */
    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        // Ignore bot messages
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String channelId = message.getChannel().getId();
        String guildId = event.isFromGuild() ? event.getGuild().getId() : null;

        log.debug("Message updated by {} in channel {}: {}",
                event.getAuthor().getName(),
                message.getChannel().getName(),
                message.getContentDisplay());

        // Convert to DTO
        MessageDTO messageDTO = convertToMessageDTO(message);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("message", messageDTO);
        eventData.put("channelId", channelId);
        if (guildId != null) {
            eventData.put("guildId", guildId);
        }

        // Broadcast to subscribed clients
        eventHandler.broadcastToChannel(channelId, "MESSAGE_UPDATE", eventData);
        if (guildId != null) {
            eventHandler.broadcastToGuild(guildId, "MESSAGE_UPDATE", eventData);
        }
    }

    /**
     * Handle message delete events
     */
    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        String messageId = event.getMessageId();
        String channelId = event.getChannel().getId();
        String guildId = event.isFromGuild() ? event.getGuild().getId() : null;

        log.debug("Message deleted in channel {}, message ID: {}",
                event.getChannel().getName(),
                messageId);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("messageId", messageId);
        eventData.put("channelId", channelId);
        if (guildId != null) {
            eventData.put("guildId", guildId);
        }

        // Broadcast to subscribed clients
        eventHandler.broadcastToChannel(channelId, "MESSAGE_DELETE", eventData);
        if (guildId != null) {
            eventHandler.broadcastToGuild(guildId, "MESSAGE_DELETE", eventData);
        }
    }

    /**
     * Handle reaction add events
     */
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        // Ignore self-reactions
        if (event.getUserId().equals(event.getJDA().getSelfUser().getId())) return;

        String emoji = event.getReaction().getEmoji().getAsReactionCode();
        String messageId = event.getMessageId();
        String channelId = event.getChannel().getId();
        String userId = event.getUserId();
        String guildId = event.isFromGuild() ? event.getGuild().getId() : null;

        log.debug("Reaction {} added to message {} in channel {} by user {}",
                emoji, messageId, event.getChannel().getName(), userId);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("messageId", messageId);
        eventData.put("channelId", channelId);
        eventData.put("userId", userId);
        eventData.put("emoji", emoji);
        if (guildId != null) {
            eventData.put("guildId", guildId);
        }

        // Broadcast to subscribed clients
        eventHandler.broadcastToChannel(channelId, "MESSAGE_REACTION_ADD", eventData);
        if (guildId != null) {
            eventHandler.broadcastToGuild(guildId, "MESSAGE_REACTION_ADD", eventData);
        }
    }

    /**
     * Handle reaction remove events
     */
    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        // Ignore self-reactions
        if (event.getUserId().equals(event.getJDA().getSelfUser().getId())) return;

        String emoji = event.getReaction().getEmoji().getAsReactionCode();
        String messageId = event.getMessageId();
        String channelId = event.getChannel().getId();
        String userId = event.getUserId();
        String guildId = event.isFromGuild() ? event.getGuild().getId() : null;

        log.debug("Reaction {} removed from message {} in channel {} by user {}",
                emoji, messageId, event.getChannel().getName(), userId);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("messageId", messageId);
        eventData.put("channelId", channelId);
        eventData.put("userId", userId);
        eventData.put("emoji", emoji);
        if (guildId != null) {
            eventData.put("guildId", guildId);
        }

        // Broadcast to subscribed clients
        eventHandler.broadcastToChannel(channelId, "MESSAGE_REACTION_REMOVE", eventData);
        if (guildId != null) {
            eventHandler.broadcastToGuild(guildId, "MESSAGE_REACTION_REMOVE", eventData);
        }
    }

    /**
     * Process a message that might contain a command
     * This is a helper method to structure command processing
     *
     * @param message The message that might contain a command
     * @return CompletableFuture that completes when command is processed
     */
    private CompletableFuture<Void> processCommand(Message message) {
        String content = message.getContentRaw();
        if (!content.startsWith("!")) {
            return CompletableFuture.completedFuture(null);
        }

        String[] parts = content.substring(1).split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        String channelId = message.getChannel().getId();

        // Process different commands
        switch (command) {
            case "ping":
                return messageService.sendMessage(channelId, "Pong!")
                        .thenAccept(dto -> {}) // Consumes the MessageDTO and returns CompletableFuture<Void>
                        .exceptionally(ex -> {
                            log.error("Failed to send ping response", ex);
                            return null;
                        });

            case "help":
                return messageService.sendMessage(channelId, "Available commands: !ping, !help")
                        .thenAccept(dto -> {})
                        .exceptionally(ex -> {
                            log.error("Failed to send help response", ex);
                            return null;
                        });

            case "echo":
                return messageService.sendMessage(channelId, args)
                        .thenAccept(dto -> {}) // Consumes the MessageDTO and returns CompletableFuture<Void>
                        .exceptionally(ex -> {
                            log.error("Failed to echo message", ex);
                            return null;
                        });

            case "delete":
                try {
                    String messageId = args.trim();
                    return messageService.deleteMessage(channelId, messageId)
                            .exceptionally(ex -> {
                                log.error("Failed to delete message", ex);
                                return null;
                            });
                } catch (Exception e) {
                    log.error("Invalid delete command format", e);
                    return messageService.sendMessage(channelId, "Usage: !delete [message-id]")
                            .thenApply(dto -> null);
                }

            default:
                // Unknown command
                return messageService.sendMessage(channelId, "Unknown command: " + command)
                        .thenAccept(dto -> {})  // Convert CompletableFuture<MessageDTO> to CompletableFuture<Void>
                        .exceptionally(ex -> {
                            log.error("Failed to send unknown command message", ex);
                            return null;
                        });
        }
    }

    /**
     * Convert a JDA Message to MessageDTO
     *
     * @param message The JDA Message object
     * @return MessageDTO representation of the message
     */
    private MessageDTO convertToMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .content(message.getContentRaw())
                .channelId(message.getChannel().getId())
                .authorId(message.getAuthor().getId())
                .authorName(message.getAuthor().getName())
                .authorAvatarUrl(message.getAuthor().getEffectiveAvatarUrl())
                .timestamp(message.getTimeCreated().toInstant().toEpochMilli())
                .edited(message.isEdited())
                .editedTimestamp(message.getTimeEdited() != null ?
                        message.getTimeEdited().toInstant().toEpochMilli() : null)
                .attachments(convertAttachments(message))
                .referencedMessageId(message.getReferencedMessage() != null ?
                        message.getReferencedMessage().getId() : null)
                .build();
    }

    /**
     * Convert JDA Message Attachments to DTOs
     *
     * @param message The JDA Message with attachments
     * @return List of AttachmentDTOs
     */
    private List<MessageDTO.AttachmentDTO> convertAttachments(Message message) {
        if (message.getAttachments().isEmpty()) {
            return null;
        }

        return message.getAttachments().stream()
                .map(attachment -> MessageDTO.AttachmentDTO.builder()
                        .id(attachment.getId())
                        .fileName(attachment.getFileName())
                        .url(attachment.getUrl())
                        .size(Long.valueOf(attachment.getSize()))
                        .width(attachment.getWidth() > 0 ? attachment.getWidth() : null)
                        .height(attachment.getHeight() > 0 ? attachment.getHeight() : null)
                        .build())
                .collect(Collectors.toList());
    }
}