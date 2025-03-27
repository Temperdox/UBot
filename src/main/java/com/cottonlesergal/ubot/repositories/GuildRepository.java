package com.cottonlesergal.ubot.repositories;

import com.cottonlesergal.ubot.entities.Guild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Guild entities.
 */
@Repository
public interface GuildRepository extends JpaRepository<Guild, String> {

    /**
     * Find guild by name (exact match, case insensitive).
     *
     * @param name The guild name
     * @return Optional containing the guild if found
     */
    Optional<Guild> findByNameIgnoreCase(String name);

    /**
     * Find guilds by name containing a specific string.
     *
     * @param namePart The partial name to search for
     * @return List of guilds matching the search term
     */
    List<Guild> findByNameContainingIgnoreCase(String namePart);

    /**
     * Find guilds by owner ID.
     *
     * @param ownerId The ID of the guild owner
     * @return List of guilds owned by the specified user
     */
    List<Guild> findByOwnerId(String ownerId);

    /**
     * Find guilds created after a specific date.
     *
     * @param date The datetime threshold
     * @return List of guilds created after the specified date
     */
    List<Guild> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find guilds updated after a specific date.
     *
     * @param date The datetime threshold
     * @return List of guilds updated after the specified date
     */
    List<Guild> findByUpdatedAtAfter(LocalDateTime date);

    /**
     * Find guilds with member count greater than a specific value.
     *
     * @param count The member count threshold
     * @return List of guilds with more members than the specified count
     */
    List<Guild> findByMemberCountGreaterThan(int count);

    /**
     * Find guilds where a specific user is a member.
     *
     * @param userId The user ID
     * @return List of guilds the user is a member of
     */
    @Query("SELECT g FROM Guild g JOIN g.members m WHERE m.id = :userId")
    List<Guild> findGuildsWithMember(@Param("userId") String userId);

    /**
     * Count guilds by owner ID.
     *
     * @param ownerId The owner ID
     * @return The count of guilds owned by the specified user
     */
    long countByOwnerId(String ownerId);
}