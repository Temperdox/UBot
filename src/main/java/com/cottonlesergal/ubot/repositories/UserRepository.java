package com.cottonlesergal.ubot.repositories;

import com.cottonlesergal.ubot.entities.Guild;
import com.cottonlesergal.ubot.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by name (case-insensitive).
     *
     * @param name The username
     * @return Optional containing the user if found
     */
    Optional<User> findByNameIgnoreCase(String name);

    /**
     * Find users by bot status.
     *
     * @param isBot Whether to find bot users (true) or human users (false)
     * @return List of users matching the bot status
     */
    List<User> findByBot(boolean isBot);

    /**
     * Find users created after a specific date.
     *
     * @param date The datetime threshold
     * @return List of users created after the specified date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find users by name containing a specific string.
     *
     * @param namePart The partial name to search for
     * @return List of users matching the search term
     */
    List<User> findByNameContainingIgnoreCase(String namePart);

    /**
     * Find users who are members of a specific guild.
     *
     * @param guild The guild entity
     * @return List of users who are members of the guild
     */
    @Query("SELECT u FROM User u JOIN u.guilds g WHERE g = :guild")
    List<User> findMembersByGuild(@Param("guild") Guild guild);

    /**
     * Find users who are members of a specific guild by ID.
     *
     * @param guildId The guild ID
     * @return List of users who are members of the guild
     */
    @Query("SELECT u FROM User u JOIN u.guilds g WHERE g.id = :guildId")
    List<User> findMembersByGuildId(@Param("guildId") String guildId);

    /**
     * Find users with active sessions.
     *
     * @return List of users with active sessions
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.sessions s WHERE s.active = true AND s.expiresAt > CURRENT_TIMESTAMP")
    List<User> findUsersWithActiveSessions();

    /**
     * Find users who have been inactive for a period.
     *
     * @param threshold The datetime threshold
     * @return List of users inactive since the specified date
     */
    @Query("SELECT u FROM User u LEFT JOIN u.sessions s WHERE s IS NULL OR (s.lastActivityAt < :threshold)")
    List<User> findInactiveUsers(@Param("threshold") LocalDateTime threshold);

    /**
     * Count users who are members of a specific guild.
     *
     * @param guildId The guild ID
     * @return The count of users who are members of the guild
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.guilds g WHERE g.id = :guildId")
    long countMembersByGuildId(@Param("guildId") String guildId);

    /**
     * Find mutual guilds between two users.
     *
     * @param userId1 The first user ID
     * @param userId2 The second user ID
     * @return List of guild IDs that both users are members of
     */
    @Query("SELECT g.id FROM Guild g JOIN g.members m1 JOIN g.members m2 WHERE m1.id = :userId1 AND m2.id = :userId2")
    List<String> findMutualGuildIds(@Param("userId1") String userId1, @Param("userId2") String userId2);
}