package com.cottonlesergal.ubot.util;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Helper methods for entity class operations such as ID generation.
 */
public class EntityHelper {

    /**
     * Generates a random UUID string.
     *
     * @return A random UUID string
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Create a timestamp for entity creation or updates.
     *
     * @return Current local date time
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Example of how to use this in an entity class:
     *
     * <pre>
     * {@code
     * @PrePersist
     * protected void onCreate() {
     *     if (this.id == null) {
     *         this.id = EntityHelper.generateUUID();
     *     }
     *     this.createdAt = EntityHelper.now();
     *     this.updatedAt = EntityHelper.now();
     * }
     *
     * @PreUpdate
     * protected void onUpdate() {
     *     this.updatedAt = EntityHelper.now();
     * }
     * }
     * </pre>
     */
}