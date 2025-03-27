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

-- Guilds table
CREATE TABLE IF NOT EXISTS guilds (
    id VARCHAR(30) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon_url VARCHAR(255),
    description TEXT,
    owner_id VARCHAR(30),
    member_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_guild_owner (owner_id)
);

-- Channels table
CREATE TABLE IF NOT EXISTS channels (
    id VARCHAR(30) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    topic TEXT,
    position INT,
    nsfw BOOLEAN DEFAULT FALSE,
    parent_id VARCHAR(30),
    slow_mode_delay INT,
    guild_id VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES channels(id) ON DELETE SET NULL,
    INDEX idx_channel_guild (guild_id),
    INDEX idx_channel_parent (parent_id)
);

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(30) NOT NULL PRIMARY KEY,
    content TEXT NOT NULL,
    channel_id VARCHAR(30) NOT NULL,
    author_id VARCHAR(30) NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    edited BOOLEAN DEFAULT FALSE,
    edited_timestamp TIMESTAMP NULL,
    referenced_message_id VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE,
    INDEX idx_message_channel (channel_id),
    INDEX idx_message_author (author_id),
    INDEX idx_message_timestamp (timestamp),
    INDEX idx_message_reference (referenced_message_id)
);

-- Message attachments table
CREATE TABLE IF NOT EXISTS message_attachments (
    id VARCHAR(30) NOT NULL PRIMARY KEY,
    message_id VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    size BIGINT,
    width INT,
    height INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    INDEX idx_attachment_message (message_id)
);

-- Message embeds table
CREATE TABLE IF NOT EXISTS message_embeds (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    message_id VARCHAR(30) NOT NULL,
    title VARCHAR(256),
    description TEXT,
    url VARCHAR(255),
    color VARCHAR(10),
    timestamp TIMESTAMP,
    footer_text VARCHAR(255),
    footer_icon_url VARCHAR(255),
    image_url VARCHAR(255),
    thumbnail_url VARCHAR(255),
    author_name VARCHAR(255),
    author_url VARCHAR(255),
    author_icon_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    INDEX idx_embed_message (message_id)
);

-- Message reactions table
CREATE TABLE IF NOT EXISTS message_reactions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    message_id VARCHAR(30) NOT NULL,
    emoji VARCHAR(32) NOT NULL,
    user_id VARCHAR(30),
    reaction_count INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    INDEX idx_reaction_message (message_id),
    INDEX idx_reaction_user (user_id)
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(30) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    discriminator VARCHAR(4),
    avatar_url VARCHAR(255),
    bot BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_bot (bot)
);

-- User sessions table
CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(30) NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_session_user (user_id),
    INDEX idx_session_token (session_token),
    INDEX idx_session_expires (expires_at)
);

-- User guilds (many-to-many relationship)
CREATE TABLE IF NOT EXISTS user_guilds (
    user_id VARCHAR(30) NOT NULL,
    guild_id VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, guild_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    INDEX idx_user_guilds_user (user_id),
    INDEX idx_user_guilds_guild (guild_id)
);