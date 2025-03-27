package com.cottonlesergal.ubot.repositories;

import com.cottonlesergal.ubot.entities.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing MessageAttachment entities.
 */
@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, String> {

    /**
     * Find all attachments for a specific message.
     *
     * @param messageId The message ID
     * @return List of message attachments
     */
    List<MessageAttachment> findByMessageId(String messageId);

    /**
     * Count attachments by message ID.
     *
     * @param messageId The message ID
     * @return Number of attachments for the message
     */
    long countByMessageId(String messageId);

    /**
     * Find attachments by file name (partial match).
     *
     * @param fileName Part of the file name to match
     * @return List of matching attachments
     */
    List<MessageAttachment> findByFileNameContainingIgnoreCase(String fileName);

    /**
     * Find image attachments (based on file extension).
     *
     * @return List of image attachments
     */
    @Query("SELECT a FROM MessageAttachment a WHERE " +
            "LOWER(a.fileName) LIKE '%.jpg' OR " +
            "LOWER(a.fileName) LIKE '%.jpeg' OR " +
            "LOWER(a.fileName) LIKE '%.png' OR " +
            "LOWER(a.fileName) LIKE '%.gif' OR " +
            "LOWER(a.fileName) LIKE '%.webp'")
    List<MessageAttachment> findImageAttachments();

    /**
     * Delete all attachments for a message.
     *
     * @param messageId The message ID
     */
    void deleteByMessageId(String messageId);
}