package com.cottonlesergal.ubot.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Discord channels
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDTO {
    private String id;
    private String name;
    /*Channel type (TEXT, VOICE, etc.)*/
    private String type;
    private String guildId;
    private String categoryId;
}