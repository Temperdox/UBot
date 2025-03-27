package com.cottonlesergal.ubot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_message_activity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMessageActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(name = "guild_id", nullable = false)
    private String guildId;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}