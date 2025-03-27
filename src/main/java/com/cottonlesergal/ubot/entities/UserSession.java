package com.cottonlesergal.ubot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a user session in the system
 */
@Entity
@Table(name = "user_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        if (!this.active) {
            this.active = true;
        }
    }

    /**
     * Updates the last activity timestamp
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Checks if the session is expired
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}