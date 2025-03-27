package com.cottonlesergal.ubot.listeners;
import com.cottonlesergal.ubot.services.UserService;
import com.cottonlesergal.ubot.dtos.GuildDTO;
import com.cottonlesergal.ubot.dtos.UserDTO;
import com.cottonlesergal.ubot.websocket.WebSocketEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



/**
 * Listener for Discord guild (server) events.
 * Processes guild updates and member events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GuildListener extends ListenerAdapter {

    private final WebSocketEventHandler eventHandler;

    /**
     * Handle bot joining a new guild
     */
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();

        log.info("Bot joined guild: {} ({}), owner: {}, members: {}",
                guild.getName(), guild.getId(), guild.getOwnerId(), guild.getMemberCount());

        // Convert to DTO
        GuildDTO guildDTO = GuildDTO.builder()
                .id(guild.getId())
                .name(guild.getName())
                .iconUrl(guild.getIconUrl())
                .description(guild.getDescription())
                .memberCount(guild.getMemberCount())
                .ownerId(guild.getOwnerId())
                .features(guild.getFeatures())
                .textChannelCount(guild.getTextChannels().size())
                .voiceChannelCount(guild.getVoiceChannels().size())
                .build();

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("guild", guildDTO);

        // Broadcast to all connected clients
        eventHandler.broadcastToAll("GUILD_JOIN", eventData);
    }

    /**
     * Handle bot leaving a guild
     */
    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        Guild guild = event.getGuild();

        log.info("Bot left guild: {} ({})", guild.getName(), guild.getId());

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("guildId", guild.getId());
        eventData.put("guildName", guild.getName());

        // Broadcast to all connected clients
        eventHandler.broadcastToAll("GUILD_LEAVE", eventData);
    }

    /**
     * Handle guild name update
     */
    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        Guild guild = event.getGuild();
        String oldName = event.getOldName();
        String newName = event.getNewName();

        log.debug("Guild name updated: {} -> {} ({})", oldName, newName, guild.getId());

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("guildId", guild.getId());
        eventData.put("oldName", oldName);
        eventData.put("newName", newName);

        // Broadcast to clients subscribed to this guild
        eventHandler.broadcastToGuild(guild.getId(), "GUILD_UPDATE_NAME", eventData);
    }

    /**
     * Handle guild icon update
     */
    @Override
    public void onGuildUpdateIcon(GuildUpdateIconEvent event) {
        Guild guild = event.getGuild();
        String oldIconUrl = event.getOldIconUrl();
        String newIconUrl = event.getNewIconUrl();

        log.debug("Guild icon updated: {} ({})", guild.getName(), guild.getId());

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("guildId", guild.getId());
        eventData.put("oldIconUrl", oldIconUrl);
        eventData.put("newIconUrl", newIconUrl);

        // Broadcast to clients subscribed to this guild
        eventHandler.broadcastToGuild(guild.getId(), "GUILD_UPDATE_ICON", eventData);
    }

    /**
     * Handle member joining a guild
     */
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();

        log.debug("Member joined guild: {} ({}) in guild {} ({})",
                member.getUser().getName(), member.getId(), guild.getName(), guild.getId());

        // Convert to DTO using builder directly
        UserDTO userDTO = UserDTO.builder()
                .id(member.getId())
                .name(member.getUser().getName())
                .discriminator(member.getUser().getDiscriminator())
                .avatarUrl(member.getUser().getEffectiveAvatarUrl())
                .bot(member.getUser().isBot())
                .nickname(member.getNickname())
                .status(member.getOnlineStatus().toString())
                .roles(member.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()))
                .build();

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("member", userDTO);
        eventData.put("guildId", guild.getId());

        // Broadcast to clients subscribed to this guild
        eventHandler.broadcastToGuild(guild.getId(), "GUILD_MEMBER_JOIN", eventData);
    }

    /**
     * Handle member leaving a guild
     */
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        String userId = event.getUser().getId();
        String userName = event.getUser().getName();

        log.debug("Member left guild: {} ({}) from guild {} ({})",
                userName, userId, guild.getName(), guild.getId());

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", userId);
        eventData.put("userName", userName);
        eventData.put("guildId", guild.getId());

        // Broadcast to clients subscribed to this guild
        eventHandler.broadcastToGuild(guild.getId(), "GUILD_MEMBER_REMOVE", eventData);
    }

    /**
     * Handle member nickname changes
     */
    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        String oldNickname = event.getOldNickname();
        String newNickname = event.getNewNickname();

        log.debug("Member nickname updated: {} ({}) in guild {} ({}): {} -> {}",
                member.getUser().getName(), member.getId(),
                guild.getName(), guild.getId(),
                oldNickname, newNickname);

        // Create event data for WebSocket
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", member.getId());
        eventData.put("guildId", guild.getId());
        eventData.put("oldNickname", oldNickname);
        eventData.put("newNickname", newNickname);

        // Broadcast to clients subscribed to this guild
        eventHandler.broadcastToGuild(guild.getId(), "GUILD_MEMBER_UPDATE_NICKNAME", eventData);
    }
}