package com.cottonlesergal.ubot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "refresh_token", nullable = false, unique = true)
    private String refreshToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "refresh_expires_at", nullable = false)
    private LocalDateTime refreshExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "active")
    private boolean active;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
}