package com.cottonlesergal.ubot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a Discord guild (server) in the system
 */
@Entity
@Table(name = "guilds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guild {

    @Id
    private String id;  // Discord guild ID

    @Column(nullable = false)
    private String name;

    @Column(name = "icon_url")
    private String iconUrl;

    private String description;

    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "member_count")
    private Integer memberCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "guild", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Channel> channels = new HashSet<>();

    @ManyToMany(mappedBy = "guilds", fetch = FetchType.LAZY)
    private Set<User> members = new HashSet<>();

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