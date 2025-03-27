package com.cottonlesergal.ubot.services;

import com.cottonlesergal.ubot.dtos.UserDTO;
import com.cottonlesergal.ubot.entities.UserMessageActivity;
import com.cottonlesergal.ubot.entities.UserPreference;
import com.cottonlesergal.ubot.entities.UserStatusHistory;
import com.cottonlesergal.ubot.exceptions.DiscordApiException;
import com.cottonlesergal.ubot.repositories.UserMessageActivityRepository;
import com.cottonlesergal.ubot.repositories.UserPreferenceRepository;
import com.cottonlesergal.ubot.repositories.UserStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing Discord users
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final DiscordService discordService;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserStatusHistoryRepository userStatusHistoryRepository;
    private final UserMessageActivityRepository userMessageActivityRepository;

    /**
     * Get a user by ID
     *
     * @param userId The ID of the user
     * @return User DTO
     */
    public UserDTO getUser(String userId) {
        return discordService.getUser(userId);
    }

    /**
     * Get the currently authenticated user (bot)
     *
     * @return UserDTO of the current user
     */
    public UserDTO getCurrentUser() {
        JDA jda = discordService.getJda();
        User selfUser = jda.getSelfUser();

        return UserDTO.builder()
                .id(selfUser.getId())
                .name(selfUser.getName())
                .discriminator(selfUser.getDiscriminator())
                .avatarUrl(selfUser.getEffectiveAvatarUrl())
                .bot(true)
                .build();
    }

    /**
     * Get guild members
     *
     * @param guildId The ID of the guild
     * @return List of user DTOs
     */
    public List<UserDTO> getGuildMembers(String guildId) {
        return discordService.getGuildMembers(guildId);
    }

    /**
     * Get members with a specific role
     *
     * @param guildId The ID of the guild
     * @param roleId The ID of the role
     * @return List of user DTOs
     */
    public List<UserDTO> getMembersWithRole(String guildId, String roleId) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.getMembersWithRoles(guild.getRoleById(roleId)).stream()
                .map(member -> convertToUserDTO(member.getUser(), member))
                .collect(Collectors.toList());
    }

    /**
     * Ban a user from a guild
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param reason Reason for the ban (optional)
     * @param deleteDays Number of days of messages to delete (0-7)
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> banUser(String guildId, String userId, String reason, int deleteDays) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found: " + guildId);
        }

        // Convert userId string to UserSnowflake
        UserSnowflake userSnowflake = UserSnowflake.fromId(userId);

        // Use TimeUnit.DAYS since deleteDays parameter is in days
        return guild.ban(userSnowflake, deleteDays, TimeUnit.DAYS)
                .reason(reason) // Set reason separately using the reason() method
                .submit();
    }

    /**
     * Kick a member from a guild
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param reason Reason for the kick (optional)
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> kickMember(String guildId, String userId, String reason) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.kick(UserSnowflake.fromId(userId)).reason(reason)
                .submit()
                .exceptionally(ex -> {
                    log.error("Failed to kick member", ex);
                    throw new DiscordApiException("Failed to kick member: " + ex.getMessage());
                });
    }

    /**
     * Set a nickname for a guild member
     *
     * @param guildId The ID of the guild
     * @param userId The ID of the user
     * @param nickname The new nickname (null to remove)
     * @return CompletableFuture for the operation
     */
    public CompletableFuture<Void> setNickname(String guildId, String userId, String nickname) {
        JDA jda = discordService.getJda();
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new DiscordApiException("Guild not found with ID: " + guildId);
        }

        return guild.retrieveMemberById(userId)
                .submit()
                .thenCompose(member -> member.modifyNickname(nickname).submit())
                .exceptionally(ex -> {
                    log.error("Failed to set nickname", ex);
                    throw new DiscordApiException("Failed to set nickname: " + ex.getMessage());
                });
    }

    /**
     * Get user statistics
     *
     * @param userId The ID of the user
     * @return Map of user statistics
     */
    public Map<String, Object> getUserStatistics(String userId) {
        JDA jda = discordService.getJda();
        User user = jda.getUserById(userId);
        if (user == null) {
            try {
                user = jda.retrieveUserById(userId).complete();
            } catch (Exception e) {
                throw new DiscordApiException("User not found with ID: " + userId);
            }
        }

        Map<String, Object> statistics = new HashMap<>();

        // Basic user info
        statistics.put("id", user.getId());
        statistics.put("name", user.getName());
        statistics.put("isBot", user.isBot());

        // Guild membership counts
        List<Guild> mutualGuilds = jda.getMutualGuilds(user);
        statistics.put("guildCount", mutualGuilds.size());

        // Calculate role statistics
        Map<String, Integer> roleStats = new HashMap<>();
        for (Guild guild : mutualGuilds) {
            Member member = guild.getMember(user);
            if (member != null) {
                for (Role role : member.getRoles()) {
                    roleStats.put(role.getName(), roleStats.getOrDefault(role.getName(), 0) + 1);
                }
            }
        }
        statistics.put("roles", roleStats);

        // Get message count from database
        long messageCount = userMessageActivityRepository.countByUserId(userId);
        statistics.put("messageCount", messageCount);

        // Get message count for last 7 days
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        long recentMessageCount = userMessageActivityRepository.countByUserIdAndTimestampBetween(
                userId, sevenDaysAgo, now);
        statistics.put("recentMessageCount", recentMessageCount);

        return statistics;
    }

    /**
     * Get user preferences
     *
     * @return Map of user preferences
     */
    public Map<String, Object> getUserPreferences() {
        // Get current authenticated user ID
        String userId = getCurrentUser().getId();

        // Find or create user preferences
        UserPreference preferences = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreference newPrefs = new UserPreference();
                    newPrefs.setUserId(userId);
                    return userPreferenceRepository.save(newPrefs);
                });

        // Convert to map
        Map<String, Object> prefsMap = new HashMap<>();
        prefsMap.put("theme", preferences.getTheme());
        prefsMap.put("notifications", preferences.getNotifications());
        prefsMap.put("language", preferences.getLanguage());
        prefsMap.put("timezone", preferences.getTimezone());
        prefsMap.put("compactView", preferences.getCompactView());

        // Add custom preferences
        if (preferences.getPreferencesJson() != null) {
            prefsMap.putAll(preferences.getPreferencesJson());
        }

        return prefsMap;
    }

    /**
     * Update user preferences
     *
     * @param preferences Map of preferences to update
     * @return Map of updated preferences
     */
    public Map<String, Object> updateUserPreferences(Map<String, Object> preferences) {
        // Get current authenticated user ID
        String userId = getCurrentUser().getId();

        // Find or create user preferences
        UserPreference userPrefs = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreference newPrefs = new UserPreference();
                    newPrefs.setUserId(userId);
                    return newPrefs;
                });

        // Update standard preferences if provided
        if (preferences.containsKey("theme")) {
            userPrefs.setTheme((String) preferences.get("theme"));
        }
        if (preferences.containsKey("notifications")) {
            userPrefs.setNotifications((Boolean) preferences.get("notifications"));
        }
        if (preferences.containsKey("language")) {
            userPrefs.setLanguage((String) preferences.get("language"));
        }
        if (preferences.containsKey("timezone")) {
            userPrefs.setTimezone((String) preferences.get("timezone"));
        }
        if (preferences.containsKey("compactView")) {
            userPrefs.setCompactView((Boolean) preferences.get("compactView"));
        }

        // Handle custom preferences
        Map<String, Object> customPrefs = new HashMap<>();
        if (userPrefs.getPreferencesJson() != null) {
            customPrefs.putAll(userPrefs.getPreferencesJson());
        }

        // Update with new preferences (excluding standard ones)
        for (Map.Entry<String, Object> entry : preferences.entrySet()) {
            if (!List.of("theme", "notifications", "language", "timezone", "compactView").contains(entry.getKey())) {
                customPrefs.put(entry.getKey(), entry.getValue());
            }
        }

        userPrefs.setPreferencesJson(customPrefs);

        // Save to database
        userPreferenceRepository.save(userPrefs);

        // Log the update
        log.info("Updated user preferences for user {}: {}", userId, preferences);

        // Return the updated preferences
        return getUserPreferences();
    }

    /**
     * Get mutual guilds with a user
     *
     * @param userId The ID of the user
     * @return Map containing mutual guilds information
     */
    public Map<String, Object> getMutualGuilds(String userId) {
        JDA jda = discordService.getJda();
        User user = jda.getUserById(userId);
        if (user == null) {
            try {
                user = jda.retrieveUserById(userId).complete();
            } catch (Exception e) {
                throw new DiscordApiException("User not found with ID: " + userId);
            }
        }

        List<Guild> mutualGuilds = jda.getMutualGuilds(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("count", mutualGuilds.size());

        User finalUser = user;
        List<Map<String, Object>> guildsInfo = mutualGuilds.stream()
                .map(guild -> {
                    Map<String, Object> guildInfo = new HashMap<>();
                    guildInfo.put("id", guild.getId());
                    guildInfo.put("name", guild.getName());
                    guildInfo.put("iconUrl", guild.getIconUrl());
                    guildInfo.put("memberCount", guild.getMemberCount());

                    Member member = guild.getMember(finalUser);
                    if (member != null) {
                        guildInfo.put("joinedAt", member.getTimeJoined().toInstant().toEpochMilli());
                        guildInfo.put("nickname", member.getNickname());
                        guildInfo.put("roles", member.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toList()));
                    }

                    return guildInfo;
                })
                .collect(Collectors.toList());

        result.put("guilds", guildsInfo);

        return result;
    }

    /**
     * Get user status history
     *
     * @param userId The ID of the user
     * @param startTime The start time in milliseconds (optional)
     * @param endTime The end time in milliseconds (optional)
     * @return Map containing status history information
     */
    public Map<String, Object> getUserStatusHistory(String userId, Long startTime, Long endTime) {
        Map<String, Object> history = new HashMap<>();
        history.put("userId", userId);

        // Convert timestamps to LocalDateTime
        LocalDateTime start = startTime != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
                : LocalDateTime.now().minusDays(7);

        LocalDateTime end = endTime != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault())
                : LocalDateTime.now();

        // Set the time range in the response
        history.put("startTime", start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        history.put("endTime", end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        // Get status history from database
        List<UserStatusHistory> statusHistory = userStatusHistoryRepository
                .findByUserIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeDesc(
                        userId, start, end);

        // Convert to DTOs
        List<Map<String, Object>> entries = statusHistory.stream()
                .map(status -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", status.getId());
                    entry.put("status", status.getStatus());
                    entry.put("startTime", status.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

                    if (status.getEndTime() != null) {
                        entry.put("endTime", status.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                    }

                    entry.put("duration", status.getDuration());
                    entry.put("guildId", status.getGuildId());

                    return entry;
                })
                .collect(Collectors.toList());

        history.put("entries", entries);

        // Get status duration statistics
        List<Object[]> durations = userStatusHistoryRepository.getTotalDurationsByStatus(userId, start, end);

        Map<String, Long> durationByStatus = new HashMap<>();
        for (Object[] result : durations) {
            String status = (String) result[0];
            Long duration = (Long) result[1];
            durationByStatus.put(status, duration);
        }

        history.put("totalDurations", durationByStatus);

        return history;
    }

    /**
     * Get user message activity
     *
     * @param userId The ID of the user
     * @param startTime The start time in milliseconds (optional)
     * @param endTime The end time in milliseconds (optional)
     * @return Map containing message activity information
     */
    public Map<String, Object> getUserMessageActivity(String userId, Long startTime, Long endTime) {
        Map<String, Object> activity = new HashMap<>();
        activity.put("userId", userId);

        // Convert timestamps to LocalDateTime
        LocalDateTime start = startTime != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
                : LocalDateTime.now().minusDays(7);

        LocalDateTime end = endTime != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault())
                : LocalDateTime.now();

        // Set the time range in the response
        activity.put("startTime", start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        activity.put("endTime", end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        // Get message count by day
        List<Object[]> messagesByDayResults = userMessageActivityRepository.getMessageCountByDay(userId, start, end);

        Map<String, Integer> messagesByDay = new HashMap<>();
        for (Object[] result : messagesByDayResults) {
            String day = (String) result[0];
            Long count = (Long) result[1];
            messagesByDay.put(day, count.intValue());
        }

        activity.put("messagesByDay", messagesByDay);

        // Get message count by hour
        List<Object[]> messagesByHourResults = userMessageActivityRepository.getMessageCountByHour(userId, start, end);

        Map<Integer, Integer> messagesByHour = new HashMap<>();
        for (Object[] result : messagesByHourResults) {
            Integer hour = (Integer) result[0];
            Long count = (Long) result[1];
            messagesByHour.put(hour, count.intValue());
        }

        activity.put("messagesByHour", messagesByHour);

        // Get top channels
        List<Object[]> topChannelsResults = userMessageActivityRepository.getTopChannels(userId, start, end, 5);

        List<Map<String, Object>> topChannels = new ArrayList<>();
        for (Object[] result : topChannelsResults) {
            String channelId = (String) result[0];
            Long count = (Long) result[1];

            Map<String, Object> channelInfo = new HashMap<>();
            channelInfo.put("channelId", channelId);

            // Try to get channel name from Discord
            try {
                net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel =
                        discordService.getJda().getChannelById(MessageChannel.class, channelId);
                if (channel != null) {
                    if (channel instanceof GuildChannel guildChannel) {
                        channelInfo.put("channelName", guildChannel.getName());
                        channelInfo.put("guildId", guildChannel.getGuild().getId());
                        channelInfo.put("guildName", guildChannel.getGuild().getName());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not get channel information for {}", channelId);
            }

            channelInfo.put("messageCount", count.intValue());
            topChannels.add(channelInfo);
        }

        activity.put("topChannels", topChannels);

        // Activity summary
        long totalMessages = userMessageActivityRepository.countByUserIdAndTimestampBetween(userId, start, end);

        long daysBetween = ChronoUnit.DAYS.between(start, end) + 1; // +1 to include both start and end days
        double averagePerDay = daysBetween > 0 ? (double) totalMessages / daysBetween : 0;

        String mostActiveDay = messagesByDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Integer mostActiveHour = messagesByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalMessages", totalMessages);
        summary.put("averagePerDay", averagePerDay);
        summary.put("daysCovered", daysBetween);
        summary.put("mostActiveDay", mostActiveDay);
        summary.put("mostActiveHour", mostActiveHour);

        activity.put("summary", summary);

        return activity;
    }

    /**
     * Track a user status change
     *
     * @param userId The user ID
     * @param status The new status
     * @param guildId The guild ID (optional)
     */
    public void trackUserStatusChange(String userId, String status, String guildId) {
        // Find the last status entry for this user
        List<UserStatusHistory> history = userStatusHistoryRepository.findByUserIdOrderByStartTimeDesc(userId);

        LocalDateTime now = LocalDateTime.now();

        if (!history.isEmpty()) {
            UserStatusHistory lastStatus = history.get(0);

            // If the status is the same, no need to create a new entry
            if (lastStatus.getStatus().equals(status)) {
                return;
            }

            // Close the previous status entry
            if (lastStatus.getEndTime() == null) {
                lastStatus.setEndTime(now);

                // Calculate duration
                long durationInMillis = ChronoUnit.MILLIS.between(lastStatus.getStartTime(), now);
                lastStatus.setDuration(durationInMillis);

                userStatusHistoryRepository.save(lastStatus);
            }
        }

        // Create a new status entry
        UserStatusHistory newStatus = UserStatusHistory.builder()
                .userId(userId)
                .status(status)
                .startTime(now)
                .guildId(guildId)
                .build();

        userStatusHistoryRepository.save(newStatus);
    }

    /**
     * Track a message from a user
     *
     * @param userId The user ID
     * @param channelId The channel ID
     * @param guildId The guild ID
     * @param messageId The message ID
     * @param timestamp The message timestamp
     */
    public void trackUserMessage(String userId, String channelId, String guildId,
                                 String messageId, LocalDateTime timestamp) {
        UserMessageActivity activity = UserMessageActivity.builder()
                .userId(userId)
                .channelId(channelId)
                .guildId(guildId)
                .messageId(messageId)
                .timestamp(timestamp)
                .build();

        userMessageActivityRepository.save(activity);
    }

    // Helper method to convert a user to DTO
    private UserDTO convertToUserDTO(User user, Member member) {
        return getUserDTO(user, member);
    }

    private UserDTO getUserDTO(User user, Member member) {
        UserDTO.UserDTOBuilder builder = UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .discriminator(user.getDiscriminator())
                .avatarUrl(user.getEffectiveAvatarUrl())
                .bot(user.isBot());

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