package com.cottonlesergal.ubot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a Discord message embed
 */
@Entity
@Table(name = "message_embeds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEmbed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(nullable = true)
    private String title;

    @Lob
    @Column(nullable = true)
    private String description;

    @Column(name = "url", nullable = true)
    private String url;

    @Column(nullable = true)
    private String color;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "footer_text", nullable = true)
    private String footerText;

    @Column(name = "footer_icon_url", nullable = true)
    private String footerIconUrl;

    @Column(name = "image_url", nullable = true)
    private String imageUrl;

    @Column(name = "thumbnail_url", nullable = true)
    private String thumbnailUrl;

    @Column(name = "author_name", nullable = true)
    private String authorName;

    @Column(name = "author_url", nullable = true)
    private String authorUrl;

    @Column(name = "author_icon_url", nullable = true)
    private String authorIconUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Ensure ID is set if using UUID strategy without auto-generation
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}