package com.cottonlesergal.ubot.controllers;

import com.cottonlesergal.ubot.dtos.MessageDTO;
import com.cottonlesergal.ubot.services.DiscordService;
import com.cottonlesergal.ubot.services.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for message-related endpoints.
 * Provides API for managing Discord messages.
 */
@RestController
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {

    private final MessageService messageService;
    private final DiscordService discordService;

    @Autowired
    public MessageController(MessageService messageService, DiscordService discordService) {
        this.messageService = messageService;
        this.discordService = discordService;
    }

    /**
     * Get a message by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageDTO> getMessage(@PathVariable String id) {
        log.debug("REST request to get Message : {}", id);
        MessageDTO messageDTO = messageService.getMessage(id);
        return ResponseEntity.ok(messageDTO);
    }

    /**
     * Get messages for a channel with pagination
     */
    @GetMapping("/channel/{channelId}")
    public ResponseEntity<Map<String, Object>> getChannelMessages(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("REST request to get messages for Channel : {}", channelId);

        Pageable pageable = PageRequest.of(page, size);
        Page<MessageDTO> messagePage = messageService.getMessagesForChannel(channelId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("messages", messagePage.getContent());
        response.put("currentPage", messagePage.getNumber());
        response.put("totalItems", messagePage.getTotalElements());
        response.put("totalPages", messagePage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Get messages before a specific timestamp (for infinite scrolling)
     */
    @GetMapping("/channel/{channelId}/before/{timestamp}")
    public ResponseEntity<Map<String, Object>> getMessagesBeforeTimestamp(
            @PathVariable String channelId,
            @PathVariable long timestamp,
            @RequestParam(defaultValue = "50") int limit) {

        log.debug("REST request to get messages before timestamp {} for Channel : {}", timestamp, channelId);

        List<MessageDTO> messages = messageService.getMessagesBeforeTimestamp(channelId, timestamp, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("messages", messages);
        response.put("hasMore", messages.size() >= limit);

        return ResponseEntity.ok(response);
    }

    /**
     * Get recent messages from Discord API
     */
    @GetMapping("/recent/channel/{channelId}")
    public CompletableFuture<ResponseEntity<List<MessageDTO>>> getRecentMessages(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "50") int limit) {

        log.debug("REST request to get recent messages for Channel : {}", channelId);

        return discordService.getMessages(channelId, limit)
                .thenApply(messages -> ResponseEntity.ok(messages));
    }

    /**
     * Send a message to a channel
     */
    @PostMapping("/channel/{channelId}")
    public CompletableFuture<ResponseEntity<MessageDTO>> sendMessage(
            @PathVariable String channelId,
            @RequestBody Map<String, String> messageRequest) {

        String content = messageRequest.get("content");
        log.debug("REST request to send message to Channel : {}, content: {}", channelId, content);

        return discordService.sendMessage(channelId, content)
                .thenApply(message -> ResponseEntity.status(HttpStatus.CREATED).body(message));
    }

    /**
     * Delete a message
     */
    @DeleteMapping("/{id}/channel/{channelId}")
    public CompletableFuture<ResponseEntity<Void>> deleteMessage(
            @PathVariable String id,
            @PathVariable String channelId) {

        log.debug("REST request to delete Message : {} from Channel : {}", id, channelId);

        return discordService.deleteMessage(channelId, id)
                .thenApply(v -> ResponseEntity.noContent().build());
    }

    /**
     * Edit a message
     */
    @PutMapping("/{id}/channel/{channelId}")
    public CompletableFuture<ResponseEntity<MessageDTO>> editMessage(
            @PathVariable String id,
            @PathVariable String channelId,
            @RequestBody Map<String, String> messageRequest) {

        String newContent = messageRequest.get("content");
        log.debug("REST request to edit Message : {} in Channel : {}, new content: {}", id, channelId, newContent);

        return discordService.editMessage(channelId, id, newContent)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Add a reaction to a message
     */
    @PostMapping("/{id}/channel/{channelId}/reactions")
    public CompletableFuture<ResponseEntity<Void>> addReaction(
            @PathVariable String id,
            @PathVariable String channelId,
            @RequestBody Map<String, String> reactionRequest) {

        String emoji = reactionRequest.get("emoji");
        log.debug("REST request to add reaction {} to Message : {} in Channel : {}", emoji, id, channelId);

        return discordService.addReaction(channelId, id, emoji)
                .thenApply(v -> ResponseEntity.ok().build());
    }

    /**
     * Remove a reaction from a message
     */
    @DeleteMapping("/{id}/channel/{channelId}/reactions/{emoji}")
    public CompletableFuture<ResponseEntity<Void>> removeReaction(
            @PathVariable String id,
            @PathVariable String channelId,
            @PathVariable String emoji) {

        log.debug("REST request to remove reaction {} from Message : {} in Channel : {}", emoji, id, channelId);

        return discordService.removeReaction(channelId, id, emoji)
                .thenApply(v -> ResponseEntity.ok().build());
    }

    /**
     * Reply to a message
     */
    @PostMapping("/{id}/channel/{channelId}/reply")
    public CompletableFuture<ResponseEntity<MessageDTO>> replyToMessage(
            @PathVariable String id,
            @PathVariable String channelId,
            @RequestBody Map<String, String> messageRequest) {

        String content = messageRequest.get("content");
        log.debug("REST request to reply to Message : {} in Channel : {}, content: {}", id, channelId, content);

        return messageService.replyToMessage(channelId, id, content)
                .thenApply(message -> ResponseEntity.status(HttpStatus.CREATED).body(message));
    }

    /**
     * Search messages by content
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMessages(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to search messages with query : {}", query);

        Pageable pageable = PageRequest.of(page, size);
        Page<MessageDTO> messagePage = messageService.searchMessages(query, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("messages", messagePage.getContent());
        response.put("currentPage", messagePage.getNumber());
        response.put("totalItems", messagePage.getTotalElements());
        response.put("totalPages", messagePage.getTotalPages());

        return ResponseEntity.ok(response);
    }
}