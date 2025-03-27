package com.cottonlesergal.ubot.providers;

import com.cottonlesergal.ubot.listeners.ChannelListener;
import com.cottonlesergal.ubot.listeners.GuildListener;
import com.cottonlesergal.ubot.listeners.MessageListener;
import com.cottonlesergal.ubot.listeners.UserListener;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * Provider for the JDA instance.
 * Configures and manages the Discord bot connection.
 */
@Getter
@Component
@Slf4j
public class JDAProvider {

    private final JDA jda;

    @Autowired
    public JDAProvider(
            @Value("${discord.token}") String token,
            @Value("${discord.auto-reconnect:true}") boolean autoReconnect,
            @Lazy MessageListener messageListener,
            @Lazy GuildListener guildListener,
            @Lazy ChannelListener channelListener,
            @Lazy UserListener userListener) throws Exception {

        log.info("Initializing JDA with token: {}...", token.substring(0, Math.min(5, token.length())) + "***" + token.substring(token.length()-5));

        try {
            // Build JDA with all necessary intents and cache policies
            jda = JDABuilder.createDefault(token)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.watching("UBot Control Panel"))
                    .setAutoReconnect(autoReconnect)
                    .setEnabledIntents(EnumSet.allOf(GatewayIntent.class))
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .enableCache(EnumSet.allOf(CacheFlag.class))
                    .addEventListeners(
                            messageListener,
                            guildListener,
                            channelListener,
                            userListener
                    )
                    .build()
                    .awaitReady();

            log.info("Successfully connected to Discord as {}", jda.getSelfUser().getName());
        } catch (Exception e) {
            log.error("Failed to initialize JDA: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Proper shutdown of JDA when the application stops.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down JDA connection");
        if (jda != null) {
            jda.shutdown();
        }
    }
}