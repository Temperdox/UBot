package com.cottonlesergal.ubot.controllers;

import com.cottonlesergal.ubot.dtos.GuildDTO;
import com.cottonlesergal.ubot.dtos.UserDTO;
import com.cottonlesergal.ubot.services.DiscordService;
import com.cottonlesergal.ubot.services.GuildService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for guild-related endpoints.
 * Provides API for managing Discord guilds (servers).
 */
@RestController
@RequestMapping("/api/guilds")
@Slf4j
public class GuildController {

    private final GuildService guildService;

    @Autowired
    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    /**
     * Get all guilds the bot is a member of
     */
    @GetMapping
    public ResponseEntity<List<GuildDTO>> getAllGuilds() {
        log.debug("REST request to get all Guilds");
        List<GuildDTO> guilds = guildService.getGuilds();
        return ResponseEntity.ok(guilds);
    }

    /**
     * Get a specific guild by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<GuildDTO> getGuild(@PathVariable String id) {
        log.debug("REST request to get Guild : {}", id);
        GuildDTO guild = guildService.getGuild(id);
        return ResponseEntity.ok(guild);
    }

    /**
     * Get all members of a guild
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserDTO>> getGuildMembers(@PathVariable String id) {
        log.debug("REST request to get members of Guild : {}", id);
        List<UserDTO> members = guildService.getGuildMembers(id);
        return ResponseEntity.ok(members);
    }

    /**
     * Get a member of a guild by ID
     */
    @GetMapping("/{guildId}/members/{userId}")
    public ResponseEntity<UserDTO> getGuildMember(
            @PathVariable String guildId,
            @PathVariable String userId) {

        log.debug("REST request to get member {} of Guild : {}", userId, guildId);
        UserDTO member = guildService.getGuildMember(guildId, userId);
        return ResponseEntity.ok(member);
    }

    /**
     * Kick a member from a guild
     */
    @DeleteMapping("/{guildId}/members/{userId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable String guildId,
            @PathVariable String userId,
            @RequestParam(required = false) String reason) {

        log.debug("REST request to kick member {} from Guild : {}", userId, guildId);
        guildService.kickMember(guildId, userId, reason);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ban a member from a guild
     */
    @PostMapping("/{guildId}/bans/{userId}")
    public ResponseEntity<Void> banMember(
            @PathVariable String guildId,
            @PathVariable String userId,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "0") int deleteMessageDays) {

        log.debug("REST request to ban member {} from Guild : {}", userId, guildId);
        guildService.banMember(guildId, userId, reason, deleteMessageDays);
        return ResponseEntity.noContent().build();
    }

    /**
     * Unban a member from a guild
     */
    @DeleteMapping("/{guildId}/bans/{userId}")
    public ResponseEntity<Void> unbanMember(
            @PathVariable String guildId,
            @PathVariable String userId) {

        log.debug("REST request to unban member {} from Guild : {}", userId, guildId);
        guildService.unbanMember(guildId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get banned members of a guild
     */
    @GetMapping("/{guildId}/bans")
    public ResponseEntity<List<UserDTO>> getBannedMembers(@PathVariable String guildId) {
        log.debug("REST request to get banned members of Guild : {}", guildId);
        List<UserDTO> bannedMembers = guildService.getBannedMembers(guildId);
        return ResponseEntity.ok(bannedMembers);
    }

    /**
     * Change a member's nickname
     */
    @PutMapping("/{guildId}/members/{userId}/nickname")
    public ResponseEntity<UserDTO> changeMemberNickname(
            @PathVariable String guildId,
            @PathVariable String userId,
            @RequestParam String nickname) {

        log.debug("REST request to change nickname of member {} in Guild : {}", userId, guildId);
        UserDTO member = guildService.changeMemberNickname(guildId, userId, nickname);
        return ResponseEntity.ok(member);
    }

    /**
     * Add a role to a member
     */
    @PutMapping("/{guildId}/members/{userId}/roles/{roleId}")
    public ResponseEntity<Void> addRoleToMember(
            @PathVariable String guildId,
            @PathVariable String userId,
            @PathVariable String roleId) {

        log.debug("REST request to add role {} to member {} in Guild : {}", roleId, userId, guildId);
        guildService.addRoleToMember(guildId, userId, roleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove a role from a member
     */
    @DeleteMapping("/{guildId}/members/{userId}/roles/{roleId}")
    public ResponseEntity<Void> removeRoleFromMember(
            @PathVariable String guildId,
            @PathVariable String userId,
            @PathVariable String roleId) {

        log.debug("REST request to remove role {} from member {} in Guild : {}", roleId, userId, guildId);
        guildService.removeRoleFromMember(guildId, userId, roleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get guild statistics
     */
    @GetMapping("/{guildId}/stats")
    public ResponseEntity<Map<String, Object>> getGuildStats(@PathVariable String guildId) {
        log.debug("REST request to get statistics for Guild : {}", guildId);
        Map<String, Object> stats = guildService.getGuildStats(guildId);
        return ResponseEntity.ok(stats);
    }
}