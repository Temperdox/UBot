-- User credentials table
CREATE TABLE IF NOT EXISTS user_credentials (
                                                id VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    discord_id VARCHAR(30) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

-- Auth tokens table
CREATE TABLE IF NOT EXISTS auth_tokens (
                                           id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    refresh_token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refresh_expires_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES user_credentials(id) ON DELETE CASCADE
    );

-- User status history table
CREATE TABLE IF NOT EXISTS user_status_history (
                                                   id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL DEFAULT NULL,
    duration BIGINT,
    guild_id VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_status_user_id (user_id),
    INDEX idx_user_status_times (start_time, end_time)
    );

-- User message activity table
CREATE TABLE IF NOT EXISTS user_message_activity (
                                                     id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(30) NOT NULL,
    channel_id VARCHAR(30) NOT NULL,
    guild_id VARCHAR(30) NOT NULL,
    message_id VARCHAR(30) NOT NULL UNIQUE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_message_user_id (user_id),
    INDEX idx_user_message_channel (channel_id),
    INDEX idx_user_message_guild (guild_id),
    INDEX idx_user_message_timestamp (timestamp)
    );

-- User preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
                                                id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(30) NOT NULL UNIQUE,
    theme VARCHAR(30) DEFAULT 'dark',
    notifications BOOLEAN DEFAULT TRUE,
    language VARCHAR(10) DEFAULT 'en-US',
    timezone VARCHAR(30) DEFAULT 'UTC',
    compact_view BOOLEAN DEFAULT FALSE,
    preferences_json JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_preferences_user_id (user_id)
    );