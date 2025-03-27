package com.cottonlesergal.ubot.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object for Discord events sent through WebSocket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscordEventDTO {
    /**
     * The type of event (e.g., MESSAGE_CREATE, GUILD_MEMBER_JOIN).
     */
    private String type;

    /**
     * Event data as a map that can contain various properties depending on the event type.
     */
    private Map<String, Object> data;
}