package com.cottonlesergal.ubot.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Discord message reaction events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionEventDTO {
    /**
     * Type of reaction event
     */
    public enum ReactionEventType {
        ADD, REMOVE, REMOVE_ALL, REMOVE_EMOJI
    }

    private ReactionEventType eventType;
    private String messageId;
    private String channelId;
    private String guildId;
    private String userId;
    private String emoji;
    private Integer reactionCount;

    // For REMOVE_ALL and REMOVE_EMOJI events
    private String removedEmoji;
}