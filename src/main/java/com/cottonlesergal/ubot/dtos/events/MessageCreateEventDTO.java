package com.cottonlesergal.ubot.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Discord message creation events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateEventDTO {
    private MessageDTO message;
    private String guildId;
    private Boolean isWebhook;
    private Boolean isBotMessage;
    private Boolean isFromGuild;
}