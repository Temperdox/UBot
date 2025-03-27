package com.cottonlesergal.ubot.controllers;

import com.cottonlesergal.ubot.dtos.UserDTO;
import com.cottonlesergal.ubot.services.DiscordService;
import com.cottonlesergal.ubot.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for user-related endpoints.
 * Provides API for managing Discord users.
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final DiscordService discordService;
    private final UserService userService;

    @Autowired
    public UserController(DiscordService discordService, UserService userService) {
        this.discordService = discordService;
        this.userService = userService;
    }

    /**
     * Get a user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String id) {
        log.debug("REST request to get User : {}", id);
        UserDTO user = discordService.getUser(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get the currently authenticated user (bot)
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        log.debug("REST request to get current User");
        UserDTO user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    /**
     * Get user statistics
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics(@PathVariable String id) {
        log.debug("REST request to get statistics for User : {}", id);
        Map<String, Object> statistics = userService.getUserStatistics(id);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get user preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Object>> getUserPreferences() {
        log.debug("REST request to get user preferences");
        Map<String, Object> preferences = userService.getUserPreferences();
        return ResponseEntity.ok(preferences);
    }

    /**
     * Update user preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<Map<String, Object>> updateUserPreferences(
            @RequestBody Map<String, Object> preferences) {

        log.debug("REST request to update user preferences");
        Map<String, Object> updatedPreferences = userService.updateUserPreferences(preferences);
        return ResponseEntity.ok(updatedPreferences);
    }

    /**
     * Get mutual guilds with a user
     */
    @GetMapping("/{id}/mutual-guilds")
    public ResponseEntity<Map<String, Object>> getMutualGuilds(@PathVariable String id) {
        log.debug("REST request to get mutual guilds with User : {}", id);
        Map<String, Object> mutualGuilds = userService.getMutualGuilds(id);
        return ResponseEntity.ok(mutualGuilds);
    }

    /**
     * Get user presence status history (for statistics)
     */
    @GetMapping("/{id}/status-history")
    public ResponseEntity<Map<String, Object>> getUserStatusHistory(
            @PathVariable String id,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        log.debug("REST request to get status history for User : {}", id);
        Map<String, Object> statusHistory = userService.getUserStatusHistory(id, startTime, endTime);
        return ResponseEntity.ok(statusHistory);
    }

    /**
     * Get user message activity (for statistics)
     */
    @GetMapping("/{id}/message-activity")
    public ResponseEntity<Map<String, Object>> getUserMessageActivity(
            @PathVariable String id,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        log.debug("REST request to get message activity for User : {}", id);
        Map<String, Object> messageActivity = userService.getUserMessageActivity(id, startTime, endTime);
        return ResponseEntity.ok(messageActivity);
    }
}