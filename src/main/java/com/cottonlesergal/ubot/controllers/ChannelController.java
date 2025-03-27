package com.cottonlesergal.ubot.controllers;

import com.cottonlesergal.ubot.dtos.ChannelDTO;
import com.cottonlesergal.ubot.services.ChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for channel-related endpoints.
 * Provides API for managing Discord channels.
 */
@RestController
@RequestMapping("/api/channels")
@Slf4j
public class ChannelController {

    private final ChannelService channelService;

    @Autowired
    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    /**
     * Get a channel by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChannelDTO> getChannel(@PathVariable String id) {
        log.debug("REST request to get Channel : {}", id);
        ChannelDTO channelDTO = channelService.getChannel(id);
        return ResponseEntity.ok(channelDTO);
    }

    /**
     * Get all channels in a guild
     */
    @GetMapping("/guild/{guildId}")
    public ResponseEntity<List<ChannelDTO>> getGuildChannels(@PathVariable String guildId) {
        log.debug("REST request to get channels for Guild : {}", guildId);
        List<ChannelDTO> channels = channelService.getChannels(guildId);
        return ResponseEntity.ok(channels);
    }

    /**
     * Create a new text channel in a guild
     */
    @PostMapping("/guild/{guildId}/text")
    public ResponseEntity<CompletableFuture<ChannelDTO>> createTextChannel(
            @PathVariable String guildId,
            @RequestParam String name,
            @RequestParam(required = false) String category) {

        log.debug("REST request to create text channel {} in Guild : {}", name, guildId);
        CompletableFuture<ChannelDTO> channel = channelService.createTextChannel(guildId, name, category);
        return ResponseEntity.ok(channel);
    }

    /**
     * Create a new voice channel in a guild
     */
    @PostMapping("/guild/{guildId}/voice")
    public ResponseEntity<CompletableFuture<ChannelDTO>> createVoiceChannel(
            @PathVariable String guildId,
            @RequestParam String name,
            @RequestParam(required = false) String category) {

        log.debug("REST request to create voice channel {} in Guild : {}", name, guildId);
        CompletableFuture<ChannelDTO> channel = channelService.createVoiceChannel(guildId, name, category);
        return ResponseEntity.ok(channel);
    }

    /**
     * Delete a channel
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable String id) {
        log.debug("REST request to delete Channel : {}", id);
        channelService.deleteChannel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update a channel's name
     */
    @PutMapping("/{id}/name")
    public ResponseEntity<CompletableFuture<ChannelDTO>> updateChannelName(
            @PathVariable String id,
            @RequestParam String name) {

        log.debug("REST request to update name of Channel : {} to {}", id, name);
        CompletableFuture<ChannelDTO> channel = channelService.updateChannelName(id, name);
        return ResponseEntity.ok(channel);
    }

    /**
     * Update a channel's topic (text channels only)
     */
    @PutMapping("/{id}/topic")
    public ResponseEntity<CompletableFuture<ChannelDTO>> updateChannelTopic(
            @PathVariable String id,
            @RequestParam String topic) {

        log.debug("REST request to update topic of Channel : {}", id);
        CompletableFuture<ChannelDTO> channel = channelService.updateChannelTopic(id, topic);
        return ResponseEntity.ok(channel);
    }

    /**
     * Move a channel to a different category
     */
    @PutMapping("/{id}/category")
    public ResponseEntity<CompletableFuture<ChannelDTO>> moveChannelToCategory(
            @PathVariable String id,
            @RequestParam String categoryId) {

        log.debug("REST request to move Channel : {} to Category : {}", id, categoryId);
        CompletableFuture<ChannelDTO> channel = channelService.moveChannelToCategory(id, categoryId);
        return ResponseEntity.ok(channel);
    }
}