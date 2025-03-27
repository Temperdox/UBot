/**
 * API Client for Discord Bot Control Panel
 */
(function(window) {
    // Create global API object
    window.api = window.api || {};

    // Base URL for API requests
    const API_BASE_URL = '/api';

    // Response cache
    const responseCache = {
        data: new Map(), // endpoint -> { response, timestamp }
        maxAge: 5 * 60 * 1000, // 5 minutes
    };

    /**
     * Generic API request function with caching
     * @param {string} endpoint - API endpoint
     * @param {object} options - Fetch options
     * @param {boolean} useCache - Whether to use cache
     * @returns {Promise<*>} - Promise with response data
     */
    async function apiRequest(endpoint, options = {}, useCache = true) {
        const url = `${API_BASE_URL}/${endpoint}`;
        const cacheKey = `${url}-${JSON.stringify(options)}`;

        // Check cache if enabled
        if (useCache && responseCache.data.has(cacheKey)) {
            const { response, timestamp } = responseCache.data.get(cacheKey);
            const now = Date.now();

            if (now - timestamp < responseCache.maxAge) {
                console.log(`Using cached response for: ${url}`);
                return response;
            }
        }

        // Default options
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
        };

        // Merge options
        const requestOptions = {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers,
            },
        };

        try {
            console.log(`API Request: ${options.method || 'GET'} ${url}`);
            const response = await fetch(url, requestOptions);

            if (!response.ok) {
                throw new Error(`API request failed with status ${response.status}`);
            }

            // Check content type before trying to parse JSON
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.includes("application/json")) {
                return await response.json();
            } else {
                console.error("Received non-JSON response:", await response.text().slice(0, 100));
                throw new Error("Server returned non-JSON response");
            }
        } catch (error) {
            console.error(`API request failed: ${error.message}`, error);
            throw error;
        }
    }

    /**
     * Clear API cache
     * @param {string} endpoint - Optional specific endpoint to clear
     */
    function clearCache(endpoint = null) {
        if (endpoint) {
            // Clear specific endpoint
            const prefix = `${API_BASE_URL}/${endpoint}`;

            for (const key of responseCache.data.keys()) {
                if (key.startsWith(prefix)) {
                    responseCache.data.delete(key);
                }
            }
        } else {
            // Clear entire cache
            responseCache.data.clear();
        }
    }

    // Bot Info API
    const botApi = {
        /**
         * Get bot information
         * @returns {Promise<object>} - Bot info object
         */
        getInfo: () => apiRequest('bot/info'),

        /**
         * Get bot owner information
         * @returns {Promise<object>} - Owner info object
         */
        getOwner: () => apiRequest('bot/info/owner'),

        /**
         * Get bot statistics
         * @returns {Promise<object>} - Bot statistics object
         */
        getStats: () => apiRequest('bot/stats'),
    };

    // User API
    const userApi = {
        /**
         * Get user by ID
         * @param {string} userId - User ID
         * @returns {Promise<object>} - User object
         */
        getUser: (userId) => apiRequest(`users/${userId}`),

        /**
         * Get all users
         * @param {object} options - Query options
         * @returns {Promise<Array>} - Array of users
         */
        getUsers: (options = {}) => {
            const queryParams = new URLSearchParams();
            if (options.dm) queryParams.append('dm', 'true');
            if (options.all) queryParams.append('all', 'true');

            const queryString = queryParams.toString();
            return apiRequest(`users${queryString ? `?${queryString}` : ''}`);
        },

        /**
         * Get or create DM channel with user
         * @param {string} userId - User ID
         * @returns {Promise<object>} - DM channel object
         */
        getDmChannel: (userId) => apiRequest(`users/${userId}/dm`, { method: 'POST' }),

        /**
         * Get user statistics
         * @param {string} userId - User ID
         * @returns {Promise<object>} - User statistics object
         */
        getUserStats: (userId) => apiRequest(`users/${userId}/statistics`),

        /**
         * Get current user (bot)
         * @returns {Promise<object>} - Current user object
         */
        getCurrentUser: () => apiRequest('users/me'),
    };

    // Guild API
    const guildApi = {
        /**
         * Get all guilds
         * @returns {Promise<Array>} - Array of guilds
         */
        getGuilds: () => apiRequest('guilds'),

        /**
         * Get guild by ID
         * @param {string} guildId - Guild ID
         * @returns {Promise<object>} - Guild object
         */
        getGuild: (guildId) => apiRequest(`guilds/${guildId}`),

        /**
         * Get guild members
         * @param {string} guildId - Guild ID
         * @returns {Promise<Array>} - Array of guild members
         */
        getMembers: (guildId) => apiRequest(`guilds/${guildId}/members`),

        /**
         * Get guild channels
         * @param {string} guildId - Guild ID
         * @returns {Promise<Array>} - Array of channels
         */
        getChannels: (guildId) => apiRequest(`channels/guild/${guildId}`),

        /**
         * Get guild statistics
         * @param {string} guildId - Guild ID
         * @returns {Promise<object>} - Guild statistics object
         */
        getGuildStats: (guildId) => apiRequest(`guilds/${guildId}/stats`),
    };

    // Channel API
    const channelApi = {
        /**
         * Get channel by ID
         * @param {string} channelId - Channel ID
         * @returns {Promise<object>} - Channel object
         */
        getChannel: (channelId) => apiRequest(`channels/${channelId}`),

        /**
         * Get all channels in a guild
         * @param {string} guildId - Guild ID
         * @returns {Promise<Array>} - Array of channels
         */
        getChannels: (guildId) => apiRequest(`channels/guild/${guildId}`),

        /**
         * Create a text channel in a guild
         * @param {string} guildId - Guild ID
         * @param {string} name - Channel name
         * @param {string} category - Optional category ID
         * @returns {Promise<object>} - Created channel object
         */
        createTextChannel: (guildId, name, category = null) => {
            const queryParams = new URLSearchParams();
            queryParams.append('name', name);
            if (category) queryParams.append('category', category);

            return apiRequest(`channels/guild/${guildId}/text?${queryParams.toString()}`, {
                method: 'POST',
            });
        },

        /**
         * Delete a channel
         * @param {string} channelId - Channel ID
         * @returns {Promise<void>} - Empty response
         */
        deleteChannel: (channelId) => {
            return apiRequest(`channels/${channelId}`, {
                method: 'DELETE',
            });
        }
    };

    // Message API
    const messageApi = {
        /**
         * Get message by ID
         * @param {string} messageId - Message ID
         * @returns {Promise<object>} - Message object
         */
        getMessage: (messageId) => apiRequest(`messages/${messageId}`),

        /**
         * Get messages in a channel
         * @param {string} channelId - Channel ID
         * @param {object} options - Query options (before, after, limit)
         * @returns {Promise<Array>} - Array of messages
         */
        getMessages: (channelId, options = {}) => {
            const queryParams = new URLSearchParams();
            if (options.before) queryParams.append('before', options.before);
            if (options.after) queryParams.append('after', options.after);
            if (options.limit) queryParams.append('limit', options.limit);

            const queryString = queryParams.toString();
            return apiRequest(`messages/channel/${channelId}${queryString ? `?${queryString}` : ''}`);
        },

        /**
         * Send a message
         * @param {string} channelId - Channel ID
         * @param {string} content - Message content
         * @returns {Promise<object>} - Sent message object
         */
        sendMessage: (channelId, content) => {
            return apiRequest(`messages/channel/${channelId}`, {
                method: 'POST',
                body: JSON.stringify({ content }),
            });
        },

        /**
         * Edit a message
         * @param {string} channelId - Channel ID
         * @param {string} messageId - Message ID
         * @param {string} content - New message content
         * @returns {Promise<object>} - Updated message object
         */
        editMessage: (channelId, messageId, content) => {
            return apiRequest(`messages/${messageId}/channel/${channelId}`, {
                method: 'PUT',
                body: JSON.stringify({ content }),
            });
        },

        /**
         * Delete a message
         * @param {string} channelId - Channel ID
         * @param {string} messageId - Message ID
         * @returns {Promise<void>} - Empty response
         */
        deleteMessage: (channelId, messageId) => {
            return apiRequest(`messages/${messageId}/channel/${channelId}`, {
                method: 'DELETE',
            });
        }
    };

    // Auth API
    const authApi = {
        /**
         * Login
         * @param {string} username - Username
         * @param {string} password - Password
         * @returns {Promise<object>} - Auth token
         */
        login: (username, password) => {
            return apiRequest('auth/login', {
                method: 'POST',
                body: JSON.stringify({ username, password }),
            });
        },

        /**
         * Logout
         * @returns {Promise<void>} - Empty response
         */
        logout: () => {
            return apiRequest('auth/logout', {
                method: 'POST',
            });
        },

        /**
         * Validate token
         * @param {string} token - Auth token
         * @returns {Promise<object>} - Validation result
         */
        validateToken: (token) => {
            return apiRequest('auth/validate', {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
        }
    };

    // Assign all API methods to the global api object
    window.api.request = apiRequest;
    window.api.clearCache = clearCache;
    window.api.bot = botApi;
    window.api.user = userApi;
    window.api.guild = guildApi;
    window.api.channel = channelApi;
    window.api.message = messageApi;
    window.api.auth = authApi;
})(window);