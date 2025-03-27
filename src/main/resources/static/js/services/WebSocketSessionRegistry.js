/**
 * WebSocket Session Registry
 * Manages user sessions and subscriptions for the WebSocket server
 */
class WebSocketSessionRegistry {
    constructor() {
        // Maps to track subscriptions by different criteria
        this.sessionMap = new Map(); // sessionId -> Session object
        this.guildSubscriptions = new Map(); // guildId -> Set of sessionIds
        this.channelSubscriptions = new Map(); // channelId -> Set of sessionIds
        this.dmSubscriptions = new Map(); // userId -> Set of sessionIds
        this.userSessions = new Map(); // userId -> Set of sessionIds

        // Session cleanup interval (check every 5 minutes)
        this.cleanupInterval = setInterval(() => this.cleanupSessions(), 5 * 60 * 1000);
    }

    /**
     * Register a new session
     * @param {string} sessionId - WebSocket session ID
     * @param {string} userId - User ID
     * @param {string} username - Username
     * @returns {object} - Session object
     */
    registerSession(sessionId, userId, username) {
        const session = {
            id: sessionId,
            userId: userId,
            username: username,
            createdAt: Date.now(),
            lastActivityAt: Date.now(),
            subscriptions: {
                guilds: new Set(),
                channels: new Set(),
                dms: new Set()
            }
        };

        this.sessionMap.set(sessionId, session);

        // Add to user sessions
        if (!this.userSessions.has(userId)) {
            this.userSessions.set(userId, new Set());
        }
        this.userSessions.get(userId).add(sessionId);

        return session;
    }

    /**
     * Get a session by ID
     * @param {string} sessionId - Session ID
     * @returns {object|null} - Session object or null if not found
     */
    getSession(sessionId) {
        return this.sessionMap.get(sessionId) || null;
    }

    /**
     * Update session activity timestamp
     * @param {string} sessionId - Session ID
     */
    updateSessionActivity(sessionId) {
        const session = this.sessionMap.get(sessionId);
        if (session) {
            session.lastActivityAt = Date.now();
        }
    }

    /**
     * Remove a session
     * @param {string} sessionId - Session ID
     */
    removeSession(sessionId) {
        const session = this.sessionMap.get(sessionId);
        if (!session) return;

        // Remove from user sessions
        const userId = session.userId;
        if (this.userSessions.has(userId)) {
            this.userSessions.get(userId).delete(sessionId);
            if (this.userSessions.get(userId).size === 0) {
                this.userSessions.delete(userId);
            }
        }

        // Remove from guild subscriptions
        session.subscriptions.guilds.forEach(guildId => {
            if (this.guildSubscriptions.has(guildId)) {
                this.guildSubscriptions.get(guildId).delete(sessionId);
                if (this.guildSubscriptions.get(guildId).size === 0) {
                    this.guildSubscriptions.delete(guildId);
                }
            }
        });

        // Remove from channel subscriptions
        session.subscriptions.channels.forEach(channelId => {
            if (this.channelSubscriptions.has(channelId)) {
                this.channelSubscriptions.get(channelId).delete(sessionId);
                if (this.channelSubscriptions.get(channelId).size === 0) {
                    this.channelSubscriptions.delete(channelId);
                }
            }
        });

        // Remove from DM subscriptions
        session.subscriptions.dms.forEach(userId => {
            if (this.dmSubscriptions.has(userId)) {
                this.dmSubscriptions.get(userId).delete(sessionId);
                if (this.dmSubscriptions.get(userId).size === 0) {
                    this.dmSubscriptions.delete(userId);
                }
            }
        });

        // Remove the session itself
        this.sessionMap.delete(sessionId);
    }

    /**
     * Add guild subscription
     * @param {string} guildId - Guild ID
     * @param {string} sessionId - Session ID
     */
    addGuildSubscription(guildId, sessionId) {
        const session = this.sessionMap.get(sessionId);
        if (!session) return;

        // Add to session's guild subscriptions
        session.subscriptions.guilds.add(guildId);

        // Add to guild subscriptions map
        if (!this.guildSubscriptions.has(guildId)) {
            this.guildSubscriptions.set(guildId, new Set());
        }
        this.guildSubscriptions.get(guildId).add(sessionId);

        // Update activity
        this.updateSessionActivity(sessionId);
    }

    /**
     * Add channel subscription
     * @param {string} channelId - Channel ID
     * @param {string} sessionId - Session ID
     */
    addChannelSubscription(channelId, sessionId) {
        const session = this.sessionMap.get(sessionId);
        if (!session) return;

        // Add to session's channel subscriptions
        session.subscriptions.channels.add(channelId);

        // Add to channel subscriptions map
        if (!this.channelSubscriptions.has(channelId)) {
            this.channelSubscriptions.set(channelId, new Set());
        }
        this.channelSubscriptions.get(channelId).add(sessionId);

        // Update activity
        this.updateSessionActivity(sessionId);
    }

    /**
     * Add DM subscription
     * @param {string} userId - User ID to subscribe to DMs with
     * @param {string} sessionId - Session ID
     */
    addDmSubscription(userId, sessionId) {
        const session = this.sessionMap.get(sessionId);
        if (!session) return;

        // Add to session's DM subscriptions
        session.subscriptions.dms.add(userId);

        // Add to DM subscriptions map
        if (!this.dmSubscriptions.has(userId)) {
            this.dmSubscriptions.set(userId, new Set());
        }
        this.dmSubscriptions.get(userId).add(sessionId);

        // Update activity
        this.updateSessionActivity(sessionId);
    }

    /**
     * Remove guild subscription
     * @param {string} guildId - Guild ID
     * @param {string} sessionId - Session ID
     */
    removeGuildSubscription(guildId, sessionId) {
        const session = this.sessionMap.get(sessionId);
        if (!session) return;

        // Remove from session's guild subscriptions
        session.subscriptions.guilds.delete(guildId);

        // Remove from guild subscriptions map
        if (this.guildSubscriptions.has(guildId)) {
            this.guildSubscriptions.get(guildId).delete(sessionId);
            if (this.guildSubscriptions.get(guildId).size === 0) {
                this.guildSubscriptions.delete(guildId);
            }
        }
    }

    /**
     * Remove channel subscription
     * @param {string} channelId - Channel ID
     * @param {string} sessionId - Session ID
     */
    removeChannelSubscription(channelId, sessionId) {
        const session = this.sessionMap.get(sessionId);
        if (!session) return;

        // Remove from session's channel subscriptions
        session.subscriptions.channels.delete(channelId);

        // Remove from channel subscriptions map
        if (this.channelSubscriptions.has(channelId)) {
            this.channelSubscriptions.get(channelId).delete(sessionId);
            if (this.channelSubscriptions.get(channelId).size === 0) {
                this.channelSubscriptions.delete(channelId);
            }
        }
    }

    /**
     * Remove DM subscription
     * @param {string} userId - User ID
     * @param {string} sessionId - Session ID
     */
    removeDmSubscription(userId, sessionId) {
        const session = this.sessionMap.get(sessionId);
        if (!session) return;

        // Remove from session's DM subscriptions
        session.subscriptions.dms.delete(userId);

        // Remove from DM subscriptions map
        if (this.dmSubscriptions.has(userId)) {
            this.dmSubscriptions.get(userId).delete(sessionId);
            if (this.dmSubscriptions.get(userId).size === 0) {
                this.dmSubscriptions.delete(userId);
            }
        }
    }

    /**
     * Get all sessions subscribed to a guild
     * @param {string} guildId - Guild ID
     * @returns {Array} - Array of session objects
     */
    getGuildSubscribers(guildId) {
        if (!this.guildSubscriptions.has(guildId)) {
            return [];
        }

        return Array.from(this.guildSubscriptions.get(guildId))
            .map(sessionId => this.sessionMap.get(sessionId))
            .filter(Boolean); // Filter out any null sessions
    }

    /**
     * Get all sessions subscribed to a channel
     * @param {string} channelId - Channel ID
     * @returns {Array} - Array of session objects
     */
    getChannelSubscribers(channelId) {
        if (!this.channelSubscriptions.has(channelId)) {
            return [];
        }

        return Array.from(this.channelSubscriptions.get(channelId))
            .map(sessionId => this.sessionMap.get(sessionId))
            .filter(Boolean); // Filter out any null sessions
    }

    /**
     * Get all sessions subscribed to DMs with a user
     * @param {string} userId - User ID
     * @returns {Array} - Array of session objects
     */
    getDmSubscribers(userId) {
        if (!this.dmSubscriptions.has(userId)) {
            return [];
        }

        return Array.from(this.dmSubscriptions.get(userId))
            .map(sessionId => this.sessionMap.get(sessionId))
            .filter(Boolean); // Filter out any null sessions
    }

    /**
     * Get all sessions for a user
     * @param {string} userId - User ID
     * @returns {Array} - Array of session objects
     */
    getUserSessions(userId) {
        if (!this.userSessions.has(userId)) {
            return [];
        }

        return Array.from(this.userSessions.get(userId))
            .map(sessionId => this.sessionMap.get(sessionId))
            .filter(Boolean); // Filter out any null sessions
    }

    /**
     * Check if a user is online (has active sessions)
     * @param {string} userId - User ID
     * @returns {boolean} - True if the user has any active sessions
     */
    isUserOnline(userId) {
        return this.userSessions.has(userId) && this.userSessions.get(userId).size > 0;
    }

    /**
     * Get count of active sessions
     * @returns {number} - Number of active sessions
     */
    getSessionCount() {
        return this.sessionMap.size;
    }

    /**
     * Clean up inactive sessions (over 24 hours old)
     */
    cleanupSessions() {
        const now = Date.now();
        const inactivityThreshold = 24 * 60 * 60 * 1000; // 24 hours

        for (const [sessionId, session] of this.sessionMap.entries()) {
            const inactiveTime = now - session.lastActivityAt;

            if (inactiveTime > inactivityThreshold) {
                console.log(`Removing inactive session: ${sessionId}, inactive for ${inactiveTime / 1000} seconds`);
                this.removeSession(sessionId);
            }
        }
    }

    /**
     * Dispose registry and clean up resources
     */
    dispose() {
        if (this.cleanupInterval) {
            clearInterval(this.cleanupInterval);
            this.cleanupInterval = null;
        }

        // Clear all data
        this.sessionMap.clear();
        this.guildSubscriptions.clear();
        this.channelSubscriptions.clear();
        this.dmSubscriptions.clear();
        this.userSessions.clear();
    }
}

// Create global instance
window.sessionRegistry = new WebSocketSessionRegistry();