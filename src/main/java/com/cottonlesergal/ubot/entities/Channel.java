package com.cottonlesergal.ubot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Discord channel in the system
 */
@Entity
@Table(name = "channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel {

    @Id
    private String id;  // Discord channel ID

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;  // TEXT, VOICE, CATEGORY, etc.

    private String topic;

    private Integer position;

    private Boolean nsfw;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "slow_mode_delay")
    private Integer slowModeDelay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_id")
    private Guild guild;

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}