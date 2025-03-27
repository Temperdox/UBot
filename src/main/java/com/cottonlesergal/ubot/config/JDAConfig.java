package com.cottonlesergal.ubot.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

/**
 * Configuration class for JDA (Java Discord API) settings.
 * Provides a centralized place for Discord bot configuration.
 */
@Configuration
public class JDAConfig {

    @Value("${discord.token}")
    private String token;

    @Value("${discord.activity.type:WATCHING}")
    private String activityType;

    @Value("${discord.activity.description:UBot Control Panel}")
    private String activityDescription;

    @Value("${discord.status:ONLINE}")
    private String status;

    @Value("${discord.auto-reconnect:true}")
    private boolean autoReconnect;

    /**
     * Configure the JDA instance with application settings.
     * This method is not used directly but documents the settings
     * that are used in JDAProvider.
     *
     * @return Configured JDABuilder (not used directly)
     */
    @Bean
    public JDABuilder jdaBuilder() {
        Activity activity = getActivity();
        OnlineStatus onlineStatus = OnlineStatus.valueOf(status);

        return JDABuilder.createDefault(token)
                .setStatus(onlineStatus)
                .setActivity(activity)
                .setAutoReconnect(autoReconnect)
                .setEnabledIntents(EnumSet.allOf(GatewayIntent.class))
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableCache(EnumSet.allOf(CacheFlag.class));
    }

    /**
     * Creates an activity object based on configuration.
     *
     * @return The configured Activity
     */
    private Activity getActivity() {
        switch (activityType.toUpperCase()) {
            case "PLAYING":
                return Activity.playing(activityDescription);
            case "LISTENING":
                return Activity.listening(activityDescription);
            case "COMPETING":
                return Activity.competing(activityDescription);
            case "STREAMING":
                return Activity.streaming(activityDescription, "https://www.twitch.tv/placeholder");
            case "WATCHING":
            default:
                return Activity.watching(activityDescription);
        }
    }
}