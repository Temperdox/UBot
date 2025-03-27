/**
 * Mutations for Discord Bot Control Panel
 * Functions to modify the application state
 */

import { state } from './state.js';

// Event listeners for state changes
const listeners = {};

/**
 * Set state value
 * @param {string} key - State key to update
 * @param {*} value - New value
 * @param {boolean} merge - Whether to merge with existing object (for objects only)
 */
function setState(key, value, merge = false) {
    // Handle nested keys with dot notation (e.g., 'status.connected')
    if (key.includes('.')) {
        const parts = key.split('.');
        let current = state;

        // Navigate to the nested object
        for (let i = 0; i < parts.length - 1; i++) {
            if (!current[parts[i]]) {
                current[parts[i]] = {};
            }
            current = current[parts[i]];
        }

        const lastKey = parts[parts.length - 1];

        if (merge && typeof current[lastKey] === 'object' && current[lastKey] !== null && typeof value === 'object') {
            // Merge objects
            current[lastKey] = { ...current[lastKey], ...value };
        } else {
            // Set value directly
            current[lastKey] = value;
        }
    } else {
        if (merge && typeof state[key] === 'object' && state[key] !== null && typeof value === 'object') {
            // Merge objects
            state[key] = { ...state[key], ...value };
        } else {
            // Set value directly
            state[key] = value;
        }
    }

    // Notify listeners
    notifyListeners(key);
}

/**
 * Subscribe to state changes
 * @param {string} key - State key to subscribe to
 * @param {function} callback - Function to call when state changes
 * @returns {function} - Unsubscribe function
 */
function subscribe(key, callback) {
    if (!listeners[key]) {
        listeners[key] = [];
    }

    listeners[key].push(callback);

    // Return unsubscribe function
    return () => {
        listeners[key] = listeners[key].filter(cb => cb !== callback);
    };
}

/**
 * Notify listeners of state changes
 * @param {string} key - State key that changed
 */
function notifyListeners(key) {
    // Get all keys that could be affected (including parent keys)
    const keys = [key];
    if (key.includes('.')) {
        const parts = key.split('.');
        let path = '';
        for (const part of parts) {
            path = path ? `${path}.${part}` : part;
            if (path !== key) { // Avoid duplication
                keys.push(path);
            }
        }
    }

    // Notify listeners for each affected key
    keys.forEach(k => {
        if (listeners[k]) {
            const value = k.includes('.') ? getNestedValue(state, k) : state[k];
            listeners[k].forEach(callback => {
                try {
                    callback(value);
                } catch (error) {
                    console.error(`Error in listener for ${k}:`, error);
                }
            });
        }
    });

    // Notify global listeners
    if (listeners['*']) {
        listeners['*'].forEach(callback => {
            try {
                callback(state);
            } catch (error) {
                console.error('Error in global state listener:', error);
            }
        });
    }
}

/**
 * Get nested value from an object using dot notation
 * @param {object} obj - Object to get value from
 * @param {string} path - Path to value (e.g., 'status.connected')
 * @returns {*} - Value at path
 */
function getNestedValue(obj, path) {
    const parts = path.split('.');
    let current = obj;

    for (const part of parts) {
        if (current === undefined || current === null) {
            return undefined;
        }
        current = current[part];
    }

    return current;
}

/**
 * Set current guild
 * @param {string} guildId - Guild ID
 */
function setCurrentGuild(guildId) {
    setState('selectedGuildId', guildId);
    setState('selectedChannelId', null);
    setState('selectedDmUserId', null);
    setState('isDmView', false);
    setState('messages', []);
    clearTypingUsers();
}

/**
 * Set current channel
 * @param {string} channelId - Channel ID
 */
function setCurrentChannel(channelId) {
    setState('selectedChannelId', channelId);
    setState('messages', []);
    clearTypingUsers();
}

/**
 * Set current DM user
 * @param {string} userId - User ID
 */
function setCurrentDmUser(userId) {
    setState('selectedDmUserId', userId);
    setState('selectedGuildId', null);
    setState('selectedChannelId', null);
    setState('isDmView', true);
    setState('messages', []);
    clearTypingUsers();
}

/**
 * Switch to DM view
 */
function switchToDmView() {
    setState('isDmView', true);
    setState('selectedGuildId', null);
    setState('selectedChannelId', null);
    setState('selectedDmUserId', null);
    setState('messages', []);
    clearTypingUsers();
}

/**
 * Add a new message
 * @param {object} message - Message object
 */
function addMessage(message) {
    const messages = [...state.messages, message];

    // Sort by timestamp
    messages.sort((a, b) => a.timestamp - b.timestamp);

    setState('messages', messages);

    // Remove typing indicator for this user
    removeTypingUser(message.authorId);
}

/**
 * Update existing message
 * @param {string} messageId - Message ID
 * @param {object} updates - Updates to apply
 */
function updateMessage(messageId, updates) {
    const messages = [...state.messages];
    const index = messages.findIndex(m => m.id === messageId);

    if (index !== -1) {
        messages[index] = {
            ...messages[index],
            ...updates,
            edited: true
        };

        setState('messages', messages);
    }
}

/**
 * Delete a message
 * @param {string} messageId - Message ID
 */
function deleteMessage(messageId) {
    const messages = state.messages.filter(m => m.id !== messageId);
    setState('messages', messages);
}

/**
 * Add typing user
 * @param {string} userId - User ID
 */
function addTypingUser(userId) {
    const typingUsers = new Map(state.typingUsers);
    typingUsers.set(userId, Date.now());
    setState('typingUsers', typingUsers);
}

/**
 * Remove typing user
 * @param {string} userId - User ID
 */
function removeTypingUser(userId) {
    const typingUsers = new Map(state.typingUsers);
    typingUsers.delete(userId);
    setState('typingUsers', typingUsers);
}

/**
 * Clear all typing users
 */
function clearTypingUsers() {
    setState('typingUsers', new Map());
}

/**
 * Update user status
 * @param {string} userId - User ID
 * @param {string} status - New status
 */
function updateUserStatus(userId, status) {
    // Update in users array
    const userIndex = state.users.findIndex(u => u.id === userId);
    if (userIndex !== -1) {
        const users = [...state.users];
        users[userIndex] = { ...users[userIndex], status };
        setState('users', users);
    }

    // Update in dmUsers array
    const dmUserIndex = state.dmUsers.findIndex(u => u.id === userId);
    if (dmUserIndex !== -1) {
        const dmUsers = [...state.dmUsers];
        dmUsers[dmUserIndex] = { ...dmUsers[dmUserIndex], status };
        setState('dmUsers', dmUsers);
    }
}

/**
 * Add notification
 * @param {object} notification - Notification object
 */
function addNotification(notification) {
    const notifications = [...state.notifications, {
        id: Date.now().toString(),
        timestamp: Date.now(),
        read: false,
        ...notification
    }];

    setState('notifications', notifications);
}

/**
 * Mark notification as read
 * @param {string} notificationId - Notification ID
 */
function markNotificationAsRead(notificationId) {
    const notifications = state.notifications.map(notification =>
        notification.id === notificationId
            ? { ...notification, read: true }
            : notification
    );

    setState('notifications', notifications);
}

// Export functions
export {
    setState,
    subscribe,
    setCurrentGuild,
    setCurrentChannel,
    setCurrentDmUser,
    switchToDmView,
    addMessage,
    updateMessage,
    deleteMessage,
    addTypingUser,
    removeTypingUser,
    clearTypingUsers,
    updateUserStatus,
    addNotification,
    markNotificationAsRead
};

// For backwards compatibility with non-module code
window.mutations = {
    setState,
    subscribe,
    setCurrentGuild,
    setCurrentChannel,
    setCurrentDmUser,
    switchToDmView,
    addMessage,
    updateMessage,
    deleteMessage,
    addTypingUser,
    removeTypingUser,
    clearTypingUsers,
    updateUserStatus,
    addNotification,
    markNotificationAsRead
};