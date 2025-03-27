package com.cottonlesergal.ubot.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for Discord users
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String name;
    private String discriminator;
    private String avatarUrl;
    private boolean bot;
    private String nickname;
    private String status;
    private List<String> roles;
}