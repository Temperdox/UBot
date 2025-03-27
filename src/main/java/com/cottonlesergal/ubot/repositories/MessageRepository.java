package com.cottonlesergal.ubot.repositories;

import com.cottonlesergal.ubot.entities.Channel;
import com.cottonlesergal.ubot.entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing Message entities.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    /**
     * Find messages by channel.
     *
     * @param channel The channel entity
     * @param pageable Pagination information
     * @return Page of messages
     */
    Page<Message> findByChannel(Channel channel, Pageable pageable);

    /**
     * Find messages by channel ID.
     *
     * @param channelId The channel ID
     * @param pageable Pagination information
     * @return Page of messages
     */
    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId ORDER BY m.timestamp DESC")
    Page<Message> findByChannelId(@Param("channelId") String channelId, Pageable pageable);

    /**
     * Find messages by author ID.
     *
     * @param authorId The author ID
     * @param pageable Pagination information
     * @return Page of messages
     */
    Page<Message> findByAuthorId(String authorId, Pageable pageable);

    /**
     * Find messages by content containing a specific string.
     *
     * @param content The content to search for
     * @param pageable Pagination information
     * @return Page of messages matching the search term
     */
    @Query("SELECT m FROM Message m WHERE m.content LIKE %:term%")
    Page<Message> searchByContent(@Param("term") String term, Pageable pageable);

    /**
     * Find messages before a specific timestamp in a channel.
     *
     * @param channelId The channel ID
     * @param timestamp The timestamp threshold
     * @param limit The maximum number of messages to return
     * @return List of messages
     */
    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId AND m.timestamp < :timestamp ORDER BY m.timestamp DESC")
    List<Message> findByChannelIdBeforeTimestamp(
            @Param("channelId") String channelId,
            @Param("timestamp") LocalDateTime timestamp,
            Pageable limit);

    /**
     * Find messages after a specific timestamp in a channel.
     *
     * @param channelId The channel ID
     * @param timestamp The timestamp threshold
     * @param limit The maximum number of messages to return
     * @return List of messages
     */
    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId AND m.timestamp > :timestamp ORDER BY m.timestamp ASC")
    List<Message> findByChannelIdAfterTimestamp(
            @Param("channelId") String channelId,
            @Param("timestamp") LocalDateTime timestamp,
            Pageable limit);

    /**
     * Find messages mentioning a specific user.
     *
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of messages
     */
    @Query("SELECT m FROM Message m WHERE m.content LIKE CONCAT('%<@', :userId, '>%')")
    Page<Message> findMessagesMentioningUser(@Param("userId") String userId, Pageable pageable);

    /**
     * Count messages by author ID.
     *
     * @param authorId The author ID
     * @return The count of messages by the specified author
     */
    long countByAuthorId(String authorId);

    /**
     * Count messages by channel ID.
     *
     * @param channelId The channel ID
     * @return The count of messages in the specified channel
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.channel.id = :channelId")
    long countByChannelId(@Param("channelId") String channelId);

    /**
     * Find messages that are replies to another message.
     *
     * @param messageId The referenced message ID
     * @return List of messages that are replies to the specified message
     */
    List<Message> findByReferencedMessageId(String messageId);
}