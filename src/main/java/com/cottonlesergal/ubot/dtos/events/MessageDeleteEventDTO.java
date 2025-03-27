package com.cottonlesergal.ubot.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Discord message deletion events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDeleteEventDTO {
    private String messageId;
    private String channelId;
    private String guildId;

    // Optional cached message data that was deleted (if available)
    private MessageDTO messageData;
}