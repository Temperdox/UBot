package com.cottonlesergal.ubot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    private String theme;

    private Boolean notifications;

    private String language;

    private String timezone;

    @Column(name = "compact_view")
    private Boolean compactView;

    @Column(name = "preferences_json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> preferencesJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.theme == null) this.theme = "dark";
        if (this.notifications == null) this.notifications = true;
        if (this.language == null) this.language = "en-US";
        if (this.timezone == null) this.timezone = "UTC";
        if (this.compactView == null) this.compactView = false;
        if (this.preferencesJson == null) this.preferencesJson = new HashMap<>();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}