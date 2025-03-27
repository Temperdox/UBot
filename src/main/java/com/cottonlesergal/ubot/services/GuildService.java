package com.cottonlesergal.ubot.services;

import com.cottonlesergal.ubot.dtos.GuildDTO;
import com.cottonlesergal.ubot.dtos.UserDTO;
import com.cottonlesergal.ubot.exceptions.DiscordApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing Discord guilds (servers)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GuildService {

    private final DiscordService discordService;

    /**
     * Get all guilds the bot is connected to
     *
     * @return List of guild DTOs
     */
    public List<GuildDTO> getGuilds() {
        return discordService.getGuilds();
    }

    /**
     * Get a guild by ID
     *
     * @param guildId The ID of the guild
     * @return Guild DTO
     */
    public GuildDTO getGuild(String guildId) {
        return discordService.getGuild(guildId);
    }

    /**
     * Get all members of a guild
     *
     * @param guildId The ID of the guild
     * @return List of user DTOs
     */
    public List<UserDTO> getGuildMembers(String guildId) {
        return discordService.getGuildMembers(guildId);
    }

    /**
     * Create a role in a guild
     *
     * @param guildId The ID of the guild
     * @param roleName The name of the role
     * @param color The color for the role (can be null)
     * @return CompletableFuture with the created role ID
     */
    public CompletableFuture<String> createRole(String guildId, String roleName, Color color) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.createRole()
                .setName(roleName)
                .setColor(color)
                .submit()
                .thenApply(Role::getId)
                .exceptionally(ex -> {
                    log.error("Failed to create role", ex);
                    throw new DiscordApiException("Failed to create role: " + ex.getMessage());
                });
    }

    /**
     * Assign a role to a user
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param roleId The ID of the role
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> assignRole(String guildId, String userId, String roleId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new DiscordApiException("Role not found with ID: " + roleId);
        }

        return guild.addRoleToMember(guild.retrieveMemberById(userId).complete(), role)
                .submit()
                .exceptionally(ex -> {
                    log.error("Failed to assign role", ex);
                    throw new DiscordApiException("Failed to assign role: " + ex.getMessage());
                });
    }

    /**
     * Get all roles in a guild
     *
     * @param guildId The ID of the guild
     * @return List of role names and IDs
     */
    public List<RoleInfo> getGuildRoles(String guildId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.getRoles().stream()
                .map(role -> new RoleInfo(role.getId(), role.getName()))
                .toList();
    }

    /**
         * Simple DTO for role information
         */
        public record RoleInfo(String id, String name) {
    }

    /**
     * Get a guild member by ID
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @return UserDTO of the guild member
     */
    public UserDTO getGuildMember(String guildId, String userId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        try {
            Member member = guild.retrieveMemberById(userId).complete();
            return discordService.convertToUserDTO(member.getUser(), member);
        } catch (Exception e) {
            log.error("Failed to get guild member", e);
            throw new DiscordApiException("Member not found in guild: " + e.getMessage());
        }
    }

    /**
     * Kick a member from a guild
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user to kick
     * @param reason The reason for kicking (optional)
     */
    public void kickMember(String guildId, String userId, String reason) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        try {
            guild.kick(UserSnowflake.fromId(userId))
                    .reason(reason)
                    .complete();
            log.info("Kicked member {} from guild {} for reason: {}", userId, guildId, reason);
        } catch (Exception e) {
            log.error("Failed to kick member", e);
            throw new DiscordApiException("Failed to kick member: " + e.getMessage());
        }
    }

    /**
     * Ban a member from a guild
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user to ban
     * @param reason The reason for banning (optional)
     * @param deleteMessageDays Number of days of messages to delete (0-7)
     */
    public void banMember(String guildId, String userId, String reason, int deleteMessageDays) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        try {
            guild.ban(UserSnowflake.fromId(userId), deleteMessageDays, TimeUnit.DAYS)
                    .reason(reason)
                    .complete();
            log.info("Banned member {} from guild {} for reason: {}", userId, guildId, reason);
        } catch (Exception e) {
            log.error("Failed to ban member", e);
            throw new DiscordApiException("Failed to ban member: " + e.getMessage());
        }
    }

    /**
     * Unban a user from a guild
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user to unban
     */
    public void unbanMember(String guildId, String userId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        try {
            guild.unban(UserSnowflake.fromId(userId)).complete();
            log.info("Unbanned user {} from guild {}", userId, guildId);
        } catch (Exception e) {
            log.error("Failed to unban member", e);
            throw new DiscordApiException("Failed to unban member: " + e.getMessage());
        }
    }

    /**
     * Get all banned users in a guild
     *
     * @param guildId The ID of the guild
     * @return List of banned users as UserDTOs
     */
    public List<UserDTO> getBannedMembers(String guildId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        try {
            return guild.retrieveBanList().complete().stream()
                    .map(ban -> discordService.convertToUserDTO(ban.getUser()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to retrieve ban list", e);
            throw new DiscordApiException("Failed to retrieve ban list: " + e.getMessage());
        }
    }

    /**
     * Change a member's nickname
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param nickname The new nickname (null to remove)
     * @return UserDTO with updated information
     */
    public UserDTO changeMemberNickname(String guildId, String userId, String nickname) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        try {
            Member member = guild.retrieveMemberById(userId).complete();
            member.modifyNickname(nickname).complete();

            // Need to retrieve the member again to get updated info
            member = guild.retrieveMemberById(userId).complete();
            return discordService.convertToUserDTO(member.getUser(), member);
        } catch (Exception e) {
            log.error("Failed to change nickname", e);
            throw new DiscordApiException("Failed to change nickname: " + e.getMessage());
        }
    }

    /**
     * Add a role to a member
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param roleId The ID of the role to add
     */
    public void addRoleToMember(String guildId, String userId, String roleId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new DiscordApiException("Role not found with ID: " + roleId);
        }

        try {
            guild.addRoleToMember(UserSnowflake.fromId(userId), role).complete();
            log.info("Added role {} to member {} in guild {}", roleId, userId, guildId);
        } catch (Exception e) {
            log.error("Failed to add role to member", e);
            throw new DiscordApiException("Failed to add role to member: " + e.getMessage());
        }
    }

    /**
     * Remove a role from a member
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param roleId The ID of the role to remove
     */
    public void removeRoleFromMember(String guildId, String userId, String roleId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new DiscordApiException("Role not found with ID: " + roleId);
        }

        try {
            guild.removeRoleFromMember(UserSnowflake.fromId(userId), role).complete();
            log.info("Removed role {} from member {} in guild {}", roleId, userId, guildId);
        } catch (Exception e) {
            log.error("Failed to remove role from member", e);
            throw new DiscordApiException("Failed to remove role from member: " + e.getMessage());
        }
    }

    /**
     * Get guild statistics
     *
     * @param guildId The ID of the guild
     * @return Map of guild statistics
     */
    public Map<String, Object> getGuildStats(String guildId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        Map<String, Object> stats = new HashMap<>();

        // Basic stats
        stats.put("memberCount", guild.getMemberCount());
        stats.put("textChannelCount", guild.getTextChannels().size());
        stats.put("voiceChannelCount", guild.getVoiceChannels().size());
        stats.put("roleCount", guild.getRoles().size());
        stats.put("emojiCount", guild.getEmojis().size());
        stats.put("boostCount", guild.getBoostCount());
        stats.put("boostTier", guild.getBoostTier().getKey());

        // Online status counts
        Map<String, Long> statusCounts = guild.getMembers().stream()
                .collect(Collectors.groupingBy(
                        member -> member.getOnlineStatus().toString(),
                        Collectors.counting()
                ));
        stats.put("statusCounts", statusCounts);

        // Bot vs human counts
        long botCount = guild.getMembers().stream().filter(member -> member.getUser().isBot()).count();
        stats.put("botCount", botCount);
        stats.put("humanCount", guild.getMemberCount() - botCount);

        return stats;
    }
}