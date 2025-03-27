package com.cottonlesergal.ubot.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Data Transfer Object for Discord guilds (servers)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuildDTO {
    private String id;
    private String name;
    private String iconUrl;
    private String description;
    private int memberCount;
    private String ownerId;
    private Set<String> features;
    private int textChannelCount;
    private int voiceChannelCount;
}