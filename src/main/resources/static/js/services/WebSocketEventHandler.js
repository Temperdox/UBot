/**
 * WebSocket Event Handler for Discord Bot Control Panel
 * Handles WebSocket events and manages connections using SockJS and STOMP
 */
class WebSocketEventHandler {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.reconnectDelay = 2000; // Start with 2 seconds
        this.heartbeatInterval = null;
        this.heartbeatTimeout = null;
        this.tokenRefreshInterval = null;
        this.lastHeartbeatResponse = 0;
        this.lastUrl = null;
        this.lastToken = null;

        // Topics and subscriptions
        this.subscriptions = {};

        // Event listeners
        this.eventListeners = {};

        // Current context
        this.currentGuildId = null;
        this.currentChannelId = null;
        this.currentDmUserId = null;

        // Bind methods
        this.connect = this.connect.bind(this);
        this.disconnect = this.disconnect.bind(this);
        this.onStompMessage = this.onStompMessage.bind(this);
        this.handleMessageEvent = this.handleMessageEvent.bind(this);
        this.handleUserEvent = this.handleUserEvent.bind(this);
        this.handleGuildEvent = this.handleGuildEvent.bind(this);
        this.handleChannelEvent = this.handleChannelEvent.bind(this);
        this.reconnect = this.reconnect.bind(this);
        this.startTokenRefresh = this.startTokenRefresh.bind(this);
        this.sendPing = this.sendPing.bind(this);
    }

    /**
     * Connect to WebSocket server
     * @param {string} url - WebSocket endpoint
     * @param {string} token - Authentication token
     */
    connect(url, token = null) {
        // Clean up existing connection if any
        if (this.stompClient) {
            this.disconnect();
        }

        try {
            // Store for reconnection
            this.lastUrl = url;
            this.lastToken = token || localStorage.getItem('auth_token');

            // Create a new SockJS instance
            const socket = new SockJS(url);

            // Create STOMP client
            this.stompClient = Stomp.over(socket);

            // Disable debug logs in production
            this.stompClient.debug = null;

            console.log(`Connecting to WebSocket at: ${url}`);

            // Add auth header to handshake
            const headers = {};
            if (this.lastToken) {
                headers['Authorization'] = `Bearer ${this.lastToken}`;
            }

            // Connect to the STOMP broker
            this.stompClient.connect(
                headers,
                // On connect success
                () => {
                    console.log('WebSocket connection established');
                    this.connected = true;
                    this.reconnectAttempts = 0;
                    this.lastHeartbeatResponse = Date.now();

                    // Start heartbeat
                    this.startHeartbeat();

                    // Start token refresh
                    this.startTokenRefresh();

                    // Set up subscriptions
                    this.setupSubscriptions();

                    // Trigger connected event
                    this.trigger('connected');

                    // Resubscribe to previous context
                    this.resubscribe();
                },
                // On error
                (error) => {
                    console.error('WebSocket connection error:', error);
                    this.trigger('error', { error });
                    this.disconnect();
                    this.scheduleReconnect();
                }
            );

            // Update status
            this.trigger('connecting');
        } catch (error) {
            console.error('WebSocket initialization error:', error);
            this.trigger('error', { error });
            this.scheduleReconnect();
        }
    }

    /**
     * Set up STOMP subscriptions
     */
    setupSubscriptions() {
        if (!this.stompClient || !this.connected) return;

        // Subscribe to notifications
        this.subscriptions.notifications = this.stompClient.subscribe(
            '/user/queue/notifications',
            frame => this.onStompMessage(frame)
        );

        // Subscribe to message events
        this.subscriptions.messageEvents = this.stompClient.subscribe(
            '/topic/messages',
            frame => this.onStompMessage(frame)
        );

        // Subscribe to pong responses
        this.subscriptions.pong = this.stompClient.subscribe(
            '/user/queue/pong',
            frame => this.handlePong(frame)
        );

        // Add more subscriptions as needed
    }

    /**
     * Handle STOMP message
     * @param {Object} frame - STOMP message frame
     */
    onStompMessage(frame) {
        try {
            const data = JSON.parse(frame.body);
            console.log('WebSocket message received:', data);

            // Handle different message types
            switch (data.type) {
                case 'WELCOME':
                    this.handleWelcome(data);
                    break;

                case 'MESSAGE_CREATE':
                case 'MESSAGE_UPDATE':
                case 'MESSAGE_DELETE':
                case 'TYPING_START':
                case 'TYPING_STOP':
                    this.handleMessageEvent(data);
                    break;

                case 'PRESENCE_UPDATE':
                case 'USER_UPDATE':
                    this.handleUserEvent(data);
                    break;

                case 'GUILD_CREATE':
                case 'GUILD_UPDATE':
                case 'GUILD_DELETE':
                case 'GUILD_MEMBER_ADD':
                case 'GUILD_MEMBER_REMOVE':
                case 'GUILD_MEMBER_UPDATE':
                    this.handleGuildEvent(data);
                    break;

                case 'CHANNEL_CREATE':
                case 'CHANNEL_UPDATE':
                case 'CHANNEL_DELETE':
                    this.handleChannelEvent(data);
                    break;

                case 'PONG':
                    this.handlePong(data);
                    break;

                case 'ERROR':
                    this.handleError(data);
                    break;

                default:
                    // Trigger generic event for the message type
                    this.trigger(data.type.toLowerCase(), data);
            }

            // Always trigger the generic 'message' event
            this.trigger('message', data);
        } catch (error) {
            console.error('Error processing WebSocket message:', error, frame.body);
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    disconnect() {
        // Unsubscribe from all topics
        if (this.subscriptions) {
            Object.values(this.subscriptions).forEach(subscription => {
                if (subscription && typeof subscription.unsubscribe === 'function') {
                    subscription.unsubscribe();
                }
            });
            this.subscriptions = {};
        }

        // Disconnect STOMP client
        if (this.stompClient) {
            try {
                if (this.stompClient.connected) {
                    this.stompClient.disconnect();
                }
            } catch (error) {
                console.error('Error disconnecting STOMP client:', error);
            }
            this.stompClient = null;
        }

        // Clear heartbeat and token refresh
        this.clearHeartbeat();
        this.clearTokenRefresh();

        // Update status
        this.connected = false;
        this.trigger('disconnected');
    }

    /**
     * Schedule WebSocket reconnection
     */
    scheduleReconnect() {
        this.reconnectAttempts++;

        if (this.reconnectAttempts > this.maxReconnectAttempts) {
            console.error(`Max reconnect attempts (${this.maxReconnectAttempts}) reached`);
            this.trigger('reconnect_failed');
            return;
        }

        // Exponential backoff with jitter
        const delay = Math.min(30000, this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts - 1));
        const jitter = Math.random() * 0.5 + 0.75; // 75% to 125% of delay
        const actualDelay = Math.round(delay * jitter);

        console.log(`Scheduling reconnect attempt ${this.reconnectAttempts} in ${actualDelay}ms`);
        this.trigger('reconnecting', { attempt: this.reconnectAttempts, delay: actualDelay });

        setTimeout(this.reconnect, actualDelay);
    }

    /**
     * Attempt to reconnect
     */
    reconnect() {
        // Only reconnect if not already connected
        if (!this.connected && !this.stompClient) {
            this.connect(this.lastUrl, this.lastToken);
        }
    }

    /**
     * Start heartbeat interval
     */
    startHeartbeat() {
        this.clearHeartbeat();

        // Send heartbeat every 30 seconds
        this.heartbeatInterval = setInterval(() => {
            if (this.connected && this.stompClient && this.stompClient.connected) {
                this.sendPing();

                // Set timeout for pong response
                this.heartbeatTimeout = setTimeout(() => {
                    // If no response for 10 seconds
                    if (Date.now() - this.lastHeartbeatResponse > 10000) {
                        console.warn('Heartbeat timeout - no PONG received');
                        this.disconnect();
                        this.scheduleReconnect();
                    }
                }, 10000); // 10 second timeout
            }
        }, 30000); // 30 second interval
    }

    /**
     * Send a ping message
     */
    sendPing() {
        if (this.connected && this.stompClient && this.stompClient.connected) {
            this.send('/app/ping', { timestamp: Date.now() });
        }
    }

    /**
     * Clear heartbeat interval and timeout
     */
    clearHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }

        if (this.heartbeatTimeout) {
            clearTimeout(this.heartbeatTimeout);
            this.heartbeatTimeout = null;
        }
    }

    /**
     * Start token refresh interval
     * Refreshes token 5 minutes before expiration
     */
    startTokenRefresh() {
        this.clearTokenRefresh();

        // Check token expiration every minute
        this.tokenRefreshInterval = setInterval(() => {
            const token = localStorage.getItem('auth_token');
            if (!token) return;

            try {
                // Decode JWT to check expiration
                const payload = JSON.parse(atob(token.split('.')[1]));
                const currentTime = Math.floor(Date.now() / 1000);

                // If token expires in less than 5 minutes (300 seconds)
                if (payload.exp && payload.exp - currentTime < 300) {
                    console.log('Token expires soon, refreshing...');
                    this.refreshToken();
                }
            } catch (error) {
                console.error('Error checking token expiration:', error);
            }
        }, 60000); // Check every minute
    }

    /**
     * Refresh authentication token
     */
    async refreshToken() {
        try {
            // Get refresh token from storage
            const refreshToken = localStorage.getItem('refresh_token');
            if (!refreshToken) {
                throw new Error('No refresh token available');
            }

            // Request new token
            const response = await fetch('/api/auth/refresh-token', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (!response.ok) {
                throw new Error(`Token refresh failed: ${response.status}`);
            }

            const data = await response.json();

            // Store new token
            localStorage.setItem('auth_token', data.token);
            this.lastToken = data.token;

            // If a new refresh token is provided, store it
            if (data.refreshToken) {
                localStorage.setItem('refresh_token', data.refreshToken);
            }

            console.log('Token refreshed successfully');

            // Update connection with new token
            if (this.connected && this.stompClient) {
                // No need to disconnect and reconnect - the token is used for
                // API requests, and the websocket connection remains valid
            }

            this.trigger('token_refreshed');

        } catch (error) {
            console.error('Token refresh failed:', error);

            // If refresh fails, schedule a reconnect
            if (this.connected) {
                this.disconnect();
                this.scheduleReconnect();
            }
        }
    }

    /**
     * Clear token refresh interval
     */
    clearTokenRefresh() {
        if (this.tokenRefreshInterval) {
            clearInterval(this.tokenRefreshInterval);
            this.tokenRefreshInterval = null;
        }
    }

    /**
     * Send a message to the WebSocket server
     * @param {string} destination - STOMP destination
     * @param {object} data - Message data
     */
    send(destination, data) {
        if (this.connected && this.stompClient && this.stompClient.connected) {
            this.stompClient.send(destination, {}, JSON.stringify(data));
        } else {
            console.warn('Cannot send message - WebSocket not connected');
        }
    }

    /**
     * Set current context for subscriptions
     * @param {string} guildId - Current guild ID
     * @param {string} channelId - Current channel ID
     * @param {string} dmUserId - Current DM user ID
     */
    setContext(guildId = null, channelId = null, dmUserId = null) {
        const contextChanged =
            this.currentGuildId !== guildId ||
            this.currentChannelId !== channelId ||
            this.currentDmUserId !== dmUserId;

        if (contextChanged) {
            this.currentGuildId = guildId;
            this.currentChannelId = channelId;
            this.currentDmUserId = dmUserId;

            // Update subscriptions if connected
            if (this.connected) {
                this.resubscribe();
            }
        }
    }

    /**
     * Resubscribe to topics based on current context
     */
    resubscribe() {
        // Subscribe to guild events
        if (this.currentGuildId) {
            this.subscribeToGuild(this.currentGuildId);
        }

        // Subscribe to channel events
        if (this.currentChannelId) {
            this.subscribeToChannel(this.currentChannelId);
        }

        // Subscribe to DM events
        if (this.currentDmUserId) {
            this.subscribeToDM(this.currentDmUserId);
        }
    }

    /**
     * Subscribe to guild events
     * @param {string} guildId - Guild ID
     */
    subscribeToGuild(guildId) {
        this.send('/app/subscribe/guild', { guildId });
    }

    /**
     * Subscribe to channel events
     * @param {string} channelId - Channel ID
     */
    subscribeToChannel(channelId) {
        this.send('/app/subscribe/channel', { channelId });
    }

    /**
     * Subscribe to DM events
     * @param {string} userId - User ID
     */
    subscribeToDM(userId) {
        this.send('/app/subscribe/dm', { userId });
    }

    /**
     * Handle welcome message
     * @param {object} data - Welcome message data
     */
    handleWelcome(data) {
        console.log('Received welcome message:', data);
        this.trigger('welcome', data);

        // Update last heartbeat response time
        this.lastHeartbeatResponse = Date.now();
    }

    /**
     * Handle pong message
     * @param {object} data - Pong message data
     */
    handlePong(data) {
        // Update last heartbeat response time
        this.lastHeartbeatResponse = Date.now();

        // Clear heartbeat timeout
        if (this.heartbeatTimeout) {
            clearTimeout(this.heartbeatTimeout);
            this.heartbeatTimeout = null;
        }

        console.log('Received pong response');
        this.trigger('pong', data);
    }

    /**
     * Handle error message
     * @param {object} data - Error message data
     */
    handleError(data) {
        console.error('WebSocket error message:', data);

        // Check if it's an authentication error
        if (data.error && data.error.includes('auth')) {
            // Try to refresh token immediately
            this.refreshToken();
        }

        this.trigger('ws_error', data);
    }

    /**
     * Handle message events
     * @param {object} data - Message event data
     */
    handleMessageEvent(data) {
        // Check if this message is relevant to current context
        const isRelevant =
            (this.currentChannelId && data.channelId === this.currentChannelId) ||
            (this.currentDmUserId &&
                (data.authorId === this.currentDmUserId || data.recipientId === this.currentDmUserId));

        if (isRelevant) {
            // Trigger specific event for the message type
            this.trigger(data.type.toLowerCase(), data);

            // Also trigger generic message event
            this.trigger('message_event', data);
        }
    }

    /**
     * Handle user events
     * @param {object} data - User event data
     */
    handleUserEvent(data) {
        // Trigger specific event for the user type
        this.trigger(data.type.toLowerCase(), data);

        // Also trigger generic user event
        this.trigger('user_event', data);
    }

    /**
     * Handle guild events
     * @param {object} data - Guild event data
     */
    handleGuildEvent(data) {
        // Check if this guild event is relevant to current context
        const isRelevant = this.currentGuildId && data.guildId === this.currentGuildId;

        // Always trigger the event, but include relevance flag
        data.isRelevant = isRelevant;

        // Trigger specific event for the guild type
        this.trigger(data.type.toLowerCase(), data);

        // Also trigger generic guild event
        this.trigger('guild_event', data);
    }

    /**
     * Handle channel events
     * @param {object} data - Channel event data
     */
    handleChannelEvent(data) {
        // Check if this channel event is relevant to current context
        const isRelevant =
            (this.currentChannelId && data.channelId === this.currentChannelId) ||
            (this.currentGuildId && data.guildId === this.currentGuildId);

        // Always trigger the event, but include relevance flag
        data.isRelevant = isRelevant;

        // Trigger specific event for the channel type
        this.trigger(data.type.toLowerCase(), data);

        // Also trigger generic channel event
        this.trigger('channel_event', data);
    }

    /**
     * Send a typing indicator
     * @param {string} channelId - Channel ID
     */
    sendTyping(channelId) {
        if (channelId) {
            this.send('/app/typing', { channelId });
        }
    }

    /**
     * Add event listener
     * @param {string} event - Event name
     * @param {function} callback - Event callback
     * @returns {function} - Function to remove the listener
     */
    on(event, callback) {
        if (!this.eventListeners[event]) {
            this.eventListeners[event] = [];
        }

        this.eventListeners[event].push(callback);

        // Return a function to remove this listener
        return () => this.off(event, callback);
    }

    /**
     * Remove event listener
     * @param {string} event - Event name
     * @param {function} callback - Event callback
     */
    off(event, callback) {
        if (this.eventListeners[event]) {
            this.eventListeners[event] = this.eventListeners[event].filter(
                listener => listener !== callback
            );
        }
    }

    /**
     * Trigger an event
     * @param {string} event - Event name
     * @param {object} data - Event data
     */
    trigger(event, data = {}) {
        // Add timestamp if not present
        if (!data.timestamp) {
            data.timestamp = Date.now();
        }

        // Call listeners for this specific event
        if (this.eventListeners[event]) {
            this.eventListeners[event].forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error(`Error in event listener for ${event}:`, error);
                }
            });
        }

        // Call listeners for 'all' events
        if (this.eventListeners['all']) {
            this.eventListeners['all'].forEach(callback => {
                try {
                    callback({ type: event, data });
                } catch (error) {
                    console.error(`Error in 'all' event listener:`, error);
                }
            });
        }
    }
}

// Create global instance
window.wsHandler = new WebSocketEventHandler();