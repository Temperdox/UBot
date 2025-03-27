package com.cottonlesergal.ubot.repositories;

import com.cottonlesergal.ubot.entities.Channel;
import com.cottonlesergal.ubot.entities.Guild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Channel entities.
 */
@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

    /**
     * Find all channels belonging to a specific guild.
     *
     * @param guild The guild entity
     * @return List of channels
     */
    List<Channel> findByGuild(Guild guild);

    /**
     * Find all channels belonging to a specific guild ID.
     *
     * @param guildId The guild ID
     * @return List of channels
     */
    @Query("SELECT c FROM Channel c WHERE c.guild.id = :guildId")
    List<Channel> findByGuildId(@Param("guildId") String guildId);

    /**
     * Find channels by type.
     *
     * @param type The channel type (e.g., "TEXT", "VOICE")
     * @return List of channels of the specified type
     */
    List<Channel> findByType(String type);

    /**
     * Find channels by type in a specific guild.
     *
     * @param type The channel type
     * @param guildId The guild ID
     * @return List of channels matching the criteria
     */
    @Query("SELECT c FROM Channel c WHERE c.type = :type AND c.guild.id = :guildId")
    List<Channel> findByTypeAndGuildId(@Param("type") String type, @Param("guildId") String guildId);

    /**
     * Find channels by parent category ID.
     *
     * @param parentId The parent category ID
     * @return List of channels in the category
     */
    List<Channel> findByParentId(String parentId);

    /**
     * Find channels created after a specific date.
     *
     * @param date The datetime threshold
     * @return List of channels created after the specified date
     */
    List<Channel> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find channel by name in a specific guild.
     *
     * @param name The channel name
     * @param guildId The guild ID
     * @return Optional containing the channel if found
     */
    @Query("SELECT c FROM Channel c WHERE LOWER(c.name) = LOWER(:name) AND c.guild.id = :guildId")
    Optional<Channel> findByNameIgnoreCaseAndGuildId(@Param("name") String name, @Param("guildId") String guildId);

    /**
     * Find channels by name containing a specific string.
     *
     * @param namePart The partial name to search for
     * @return List of channels matching the search term
     */
    List<Channel> findByNameContainingIgnoreCase(String namePart);

    /**
     * Count channels by type in a specific guild.
     *
     * @param type The channel type
     * @param guildId The guild ID
     * @return The count of channels matching the criteria
     */
    @Query("SELECT COUNT(c) FROM Channel c WHERE c.type = :type AND c.guild.id = :guildId")
    long countByTypeAndGuildId(@Param("type") String type, @Param("guildId") String guildId);
}