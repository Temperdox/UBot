/**
 * Actions for Discord Bot Control Panel
 * Handles async operations and business logic
 */

import { state } from './state.js';
import * as mutations from './mutations.js';

// Cache for fetched data
const cache = {
    messages: new Map(), // channelId -> { data, timestamp }
    users: new Map(),    // guildId -> { data, timestamp }
    dmMessages: new Map() // userId -> { data, timestamp }
};

// Cache expiration time (5 minutes)
const CACHE_EXPIRATION = 5 * 60 * 1000;

/**
 * Initialize WebSocket connection
 * @param {string} wsUrl - WebSocket URL
 * @param {string} token - Authentication token
 */
async function initializeWebSocket(wsUrl, token) {
    try {
        mutations.setState('status.loading', true);

        // Initialize WebSocket handler
        if (window.wsHandler) {
            window.wsHandler.connect(wsUrl, token);

            // Set up context based on current view
            updateWebSocketContext();
        }

        mutations.setState('status.loading', false);
        return true;
    } catch (error) {
        console.error('Error initializing WebSocket:', error);
        mutations.setState('status.error', error.message);
        mutations.setState('status.loading', false);
        return false;
    }
}

/**
 * Update WebSocket context based on current selection
 */
function updateWebSocketContext() {
    if (!window.wsHandler) return;

    const { selectedGuildId, selectedChannelId, selectedDmUserId, isDmView } = state;

    if (isDmView && selectedDmUserId) {
        window.wsHandler.setContext(null, null, selectedDmUserId);
    } else if (!isDmView && selectedGuildId) {
        window.wsHandler.setContext(selectedGuildId, selectedChannelId, null);
    }
}

/**
 * Load current user (bot) information
 */
async function loadCurrentUser() {
    try {
        mutations.setState('status.loading', true);

        const response = await fetch('/api/bot/info');
        if (!response.ok) {
            throw new Error(`Failed to load bot info: ${response.status}`);
        }

        const user = await response.json();
        mutations.setState('currentUser', user);

        mutations.setState('status.loading', false);
        return user;
    } catch (error) {
        console.error('Error loading current user:', error);
        mutations.setState('status.error', error.message);
        mutations.setState('status.loading', false);

        // Return a default user object in case of error
        return {
            id: '0',
            username: 'Bot',
            discriminator: '0000',
            avatar: null,
            bot: true
        };
    }
}

/**
 * Load all guilds
 * @param {boolean} forceRefresh - Force refresh from server
 */
async function loadGuilds(forceRefresh = false) {
    try {
        mutations.setState('status.loading', true);

        const response = await fetch('/api/guilds');
        if (!response.ok) {
            throw new Error(`Failed to load guilds: ${response.status}`);
        }

        const guilds = await response.json();
        mutations.setState('guilds', guilds);

        mutations.setState('status.loading', false);
        return guilds;
    } catch (error) {
        console.error('Error loading guilds:', error);
        mutations.setState('status.error', error.message);
        mutations.setState('status.loading', false);
        return [];
    }
}

/**
 * Load channels for a guild
 * @param {string} guildId - Guild ID
 * @param {boolean} forceRefresh - Force refresh from server
 */
async function loadChannels(guildId, forceRefresh = false) {
    try {
        mutations.setState('status.loading', true);

        const response = await fetch(`/api/channels/guild/${guildId}`);
        if (!response.ok) {
            throw new Error(`Failed to load channels: ${response.status}`);
        }

        const channels = await response.json();
        mutations.setState('channels', channels);

        mutations.setState('status.loading', false);
        return channels;
    } catch (error) {
        console.error('Error loading channels:', error);
        mutations.setState('status.error', error.message);
        mutations.setState('status.loading', false);
        return [];
    }
}

/**
 * Load messages for a channel
 * @param {string} channelId - Channel ID
 * @param {object} options - Options (before, after, limit)
 * @param {boolean} forceRefresh - Force refresh from server
 */
async function loadMessages(channelId, options = {}, forceRefresh = false) {
    try {
        // Check cache first
        if (!forceRefresh && cache.messages.has(channelId)) {
            const { data, timestamp } = cache.messages.get(channelId);
            if (Date.now() - timestamp < CACHE_EXPIRATION) {
                mutations.setState('messages', data);
                return data;
            }
        }

        mutations.setState('status.loading', true);

        // Build query parameters
        const params = new URLSearchParams();
        if (options.before) params.append('before', options.before);
        if (options.after) params.append('after', options.after);
        if (options.limit) params.append('limit', options.limit);

        const queryString = params.toString();
        const url = `/api/messages/channel/${channelId}${queryString ? `?${queryString}` : ''}`;

        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Failed to load messages: ${response.status}`);
        }

        const result = await response.json();
        const messages = Array.isArray(result) ? result : (result.messages || []);

        // If loading more messages, append to existing ones
        if (options.before || options.after) {
            const existingMessages = [...state.messages];
            const combinedMessages = options.before
                ? [...messages, ...existingMessages]  // Prepend older messages
                : [...existingMessages, ...messages]; // Append newer messages

            // Filter duplicates and sort
            const uniqueMessages = Array.from(
                new Map(combinedMessages.map(m => [m.id, m])).values()
            );
            uniqueMessages.sort((a, b) => a.timestamp - b.timestamp);

            mutations.setState('messages', uniqueMessages);

            // Update cache
            cache.messages.set(channelId, {
                data: uniqueMessages,
                timestamp: Date.now()
            });
        } else {
            // Replace all messages
            mutations.setState('messages', messages);

            // Update cache
            cache.messages.set(channelId, {
                data: messages,
                timestamp: Date.now()
            });
        }

        mutations.setState('status.loading', false);
        return messages;
    } catch (error) {
        console.error('Error loading messages:', error);
        mutations.setState('status.error', error.message);
        mutations.setState('status.loading', false);

        // Try to use cached messages if available
        if (cache.messages.has(channelId)) {
            const { data } = cache.messages.get(channelId);
            mutations.setState('messages', data);
            return data;
        }

        return [];
    }
}

/**
 * Load members for a guild
 * @param {string} guildId - Guild ID
 * @param {boolean} forceRefresh - Force refresh from server
 */
async function loadGuildMembers(guildId, forceRefresh = false) {
    try {
        // Check cache first
        if (!forceRefresh && cache.users.has(guildId)) {
            const { data, timestamp } = cache.users.get(guildId);
            if (Date.now() - timestamp < CACHE_EXPIRATION) {
                mutations.setState('users', data);
                return data;
            }
        }

        mutations.setState('status.loading', true);

        const response = await fetch(`/api/guilds/${guildId}/members`);
        if (!response.ok) {
            throw new Error(`Failed to load members: ${response.status}`);
        }

        const users = await response.json();
        mutations.setState('users', users);

        // Update cache
        cache.users.set(guildId, {
            data: users,
            timestamp: Date.now()
        });

        mutations.setState('status.loading', false);
        return users;
    } catch (error) {
        console.error('Error loading guild members:', error);
        mutations.setState('status.error', error.message);
        mutations.setState('status.loading', false);

        // Try to use cached users if available
        if (cache.users.has(guildId)) {
            const { data } = cache.users.get(guildId);
            mutations.setState('users', data);
            return data;
        }

        return [];
    }
}

/**
 * Load DM users
 * @param {boolean} forceRefresh - Force refresh from server
 */
async function loadDmUsers(forceRefresh = false) {
    try {
        mutations.setState('status.loading', true);

        // First, try to get users that we can DM
        const response = await fetch('/api/users?dm=true');
        if (!response.ok) {
            throw new Error(`Failed to load DM users: ${response.status}`);
        }

        const users = await response.json();

        // If we have a bot owner, make sure they're included
        try {
            const ownerResponse = await fetch('/api/bot/info/owner');
            if (ownerResponse.ok) {
                const owner = await ownerResponse.json();
                if (owner && owner.id) {
                    // Check if owner is already in the list
                    const ownerIndex = users.findIndex(u => u.id === owner.id);
                    if (ownerIndex !== -1) {
                        // Update existing entry
                        users[ownerIndex] = {
                            ...users[ownerIndex],
                            ...owner,
                            isOwner: true
                        };
                    } else {
                        // Add to list
                        users.unshift({
                            ...owner,
                            isOwner: true
                        });
                    }
                }
            }
        } catch (ownerError) {
            console.warn('Error loading bot owner:', ownerError);
        }

        // Ensure all users have a status property
        users.forEach(user => {
            if (!user.status) {
                user.status = 'offline';
            }
        });

        mutations.setState('dmUsers', users);

        mutations.setState('status.loading', false);
        return users;
    } catch (error) {
        console.error('Error loading DM users:', error);
        mutations.setState('status.error', error.message);
        mutations.setState('status.loading', false);
        return [];
    }
}

/**
 * Load DM messages for a user
 * @param {string} userId - User ID
 * @param {boolean} forceRefresh - Force refresh from server
 */
async function loadDmMessages(userId, forceRefresh = false) {
    try {
        // Check cache first
        if (!forceRefresh && cache.dmMessages.has(userId)) {
            const { data, timestamp } = cache.dmMessages.get(userId);
            if (Date.now() - timestamp < CACHE_EXPIRATION) {
                mutations.setState('messages', data);
                return data;
            }
        }

        mutations.setState('status.loading', true);

        // First, get or create DM channel
        const channelResponse = await fetch(`/api/users/${userId}/dm`, {
            method: 'POST'
        });

        if (!channelResponse.ok) {
            throw new Error(`Failed to get DM channel: ${channelResponse.status}`);
        }

        const channel = await channelResponse.json();

        // Then load messages for this channel
        if (channel && channel.id) {
            const messagesResponse = await fetch(`/api/messages/channel/${channel.id}`);

            if (!messagesResponse.ok) {
                throw new Error(`Failed to load DM messages: ${messagesResponse.status}`);
            }

            const messages = await messagesResponse.json();

            // Sort by timestamp
            const sortedMessages = Array.isArray(messages)
                ? messages.sort((a, b) => a.timestamp - b.timestamp)
                : [];

            mutations.setState('messages', sortedMessages);

            // Update cache
            cache.dmMessages.set(userId, {
                data: sortedMessages,
                timestamp: Date.now()
            });

            mutations.setState('status.loading', false);
            return sortedMessages;
        } else {
            // No messages or channel not found
            mutations.setState('messages', []);
            mutations.setState('status.loading', false);
            return [];
        }
    } catch (error) {
        console.error('Error loading DM messages:', error);
        mutations.setState('status.error', error.message);
        mutations.setState('status.loading', false);

        // Try to use cached messages if available
        if (cache.dmMessages.has(userId)) {
            const { data } = cache.dmMessages.get(userId);
            mutations.setState('messages', data);
            return data;
        }

        return [];
    }
}

/**
 * Select a guild
 * @param {string} guildId - Guild ID
 */
async function selectGuild(guildId) {
    try {
        mutations.setCurrentGuild(guildId);
        updateWebSocketContext();

        // Load channels for this guild
        const channels = await loadChannels(guildId);

        // Find first text channel
        const firstTextChannel = channels.find(channel => channel.type === 'TEXT');

        if (firstTextChannel) {
            await selectChannel(firstTextChannel.id);
        }

        // Load guild members
        await loadGuildMembers(guildId);

        return true;
    } catch (error) {
        console.error('Error selecting guild:', error);
        mutations.setState('status.error', error.message);
        return false;
    }
}

/**
 * Select a channel
 * @param {string} channelId - Channel ID
 */
async function selectChannel(channelId) {
    try {
        mutations.setCurrentChannel(channelId);
        updateWebSocketContext();

        // Load messages for this channel
        await loadMessages(channelId);

        return true;
    } catch (error) {
        console.error('Error selecting channel:', error);
        mutations.setState('status.error', error.message);
        return false;
    }
}

/**
 * Select a DM user
 * @param {string} userId - User ID
 */
async function selectDmUser(userId) {
    try {
        mutations.setCurrentDmUser(userId);
        updateWebSocketContext();

        // Load DM messages for this user
        await loadDmMessages(userId);

        return true;
    } catch (error) {
        console.error('Error selecting DM user:', error);
        mutations.setState('status.error', error.message);
        return false;
    }
}

/**
 * Switch to DM view
 */
async function switchToDmView() {
    try {
        mutations.switchToDmView();
        updateWebSocketContext();

        // Load DM users
        await loadDmUsers();

        return true;
    } catch (error) {
        console.error('Error switching to DM view:', error);
        mutations.setState('status.error', error.message);
        return false;
    }
}

/**
 * Send a message
 * @param {string} content - Message content
 */
async function sendMessage(content) {
    try {
        if (!content || content.trim() === '') {
            return null;
        }

        const { selectedChannelId, selectedDmUserId, isDmView } = state;

        // Create temporary message for immediate feedback
        const tempMessage = {
            id: `temp-${Date.now()}`,
            content: content,
            authorId: state.currentUser?.id || '0',
            authorName: state.currentUser?.username || 'Bot',
            authorAvatarUrl: state.currentUser?.avatarUrl,
            timestamp: Date.now(),
            pending: true
        };

        mutations.addMessage(tempMessage);

        let response;

        if (isDmView && selectedDmUserId) {
            // Send DM
            // First, get or create DM channel
            const channelResponse = await fetch(`/api/users/${selectedDmUserId}/dm`, {
                method: 'POST'
            });

            if (!channelResponse.ok) {
                throw new Error(`Failed to get DM channel: ${channelResponse.status}`);
            }

            const channel = await channelResponse.json();

            // Then send message to this channel
            response = await fetch(`/api/messages/channel/${channel.id}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ content })
            });
        } else if (selectedChannelId) {
            // Send to channel
            response = await fetch(`/api/messages/channel/${selectedChannelId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ content })
            });
        } else {
            throw new Error('No channel or DM selected');
        }

        if (!response.ok) {
            throw new Error(`Failed to send message: ${response.status}`);
        }

        // Get sent message
        const message = await response.json();

        // Remove temp message
        mutations.deleteMessage(tempMessage.id);

        // Add actual message
        mutations.addMessage(message);

        // Refresh messages
        if (isDmView && selectedDmUserId) {
            await loadDmMessages(selectedDmUserId, true);
        } else if (selectedChannelId) {
            await loadMessages(selectedChannelId, {}, true);
        }

        return message;
    } catch (error) {
        console.error('Error sending message:', error);
        mutations.setState('status.error', error.message);

        // Update temporary message to show error
        const messages = state.messages.map(msg =>
            msg.id === `temp-${Date.now()}`
                ? { ...msg, error: true }
                : msg
        );
        mutations.setState('messages', messages);

        return null;
    }
}

/**
 * Edit a message
 * @param {string} messageId - Message ID
 * @param {string} content - New message content
 */
async function editMessage(messageId, content) {
    try {
        if (!content || content.trim() === '') {
            return null;
        }

        const { selectedChannelId } = state;

        if (!selectedChannelId) {
            throw new Error('No channel selected');
        }

        // Find message to edit
        const message = state.messages.find(m => m.id === messageId);
        if (!message) {
            throw new Error('Message not found');
        }

        // Update message locally first for immediate feedback
        mutations.updateMessage(messageId, { content, editing: true });

        // Send edit request
        const response = await fetch(`/api/messages/${messageId}/channel/${selectedChannelId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ content })
        });

        if (!response.ok) {
            throw new Error(`Failed to edit message: ${response.status}`);
        }

        // Get updated message
        const updatedMessage = await response.json();

        // Update message
        mutations.updateMessage(messageId, {
            ...updatedMessage,
            editing: false
        });

        return updatedMessage;
    } catch (error) {
        console.error('Error editing message:', error);
        mutations.setState('status.error', error.message);

        // Remove editing state
        const message = state.messages.find(m => m.id === messageId);
        if (message) {
            mutations.updateMessage(messageId, { editing: false, error: true });
        }

        return null;
    }
}

/**
 * Delete a message
 * @param {string} messageId - Message ID
 */
async function deleteMessage(messageId) {
    try {
        const { selectedChannelId } = state;

        if (!selectedChannelId) {
            throw new Error('No channel selected');
        }

        // Find message to delete
        const message = state.messages.find(m => m.id === messageId);
        if (!message) {
            throw new Error('Message not found');
        }

        // Delete message locally first for immediate feedback
        mutations.deleteMessage(messageId);

        // Send delete request
        const response = await fetch(`/api/messages/${messageId}/channel/${selectedChannelId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(`Failed to delete message: ${response.status}`);

            // If deletion fails, re-add the message
            mutations.addMessage(message);
        }

        return true;
    } catch (error) {
        console.error('Error deleting message:', error);
        mutations.setState('status.error', error.message);
        return false;
    }
}

/**
 * Reply to a message
 * @param {string} messageId - Message ID to reply to
 * @param {string} content - Reply content
 */
async function replyToMessage(messageId, content) {
    try {
        if (!content || content.trim() === '') {
            return null;
        }

        const { selectedChannelId } = state;

        if (!selectedChannelId) {
            throw new Error('No channel selected');
        }

        // Find message to reply to
        const message = state.messages.find(m => m.id === messageId);
        if (!message) {
            throw new Error('Message not found');
        }

        // Create temporary message for immediate feedback
        const tempMessage = {
            id: `temp-${Date.now()}`,
            content: content,
            authorId: state.currentUser?.id || '0',
            authorName: state.currentUser?.username || 'Bot',
            authorAvatarUrl: state.currentUser?.avatarUrl,
            timestamp: Date.now(),
            referencedMessageId: messageId,
            pending: true
        };

        mutations.addMessage(tempMessage);

        // Send reply request
        const response = await fetch(`/api/messages/${messageId}/channel/${selectedChannelId}/reply`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ content })
        });

        if (!response.ok) {
            throw new Error(`Failed to send reply: ${response.status}`);
        }

        // Get sent message
        const sentMessage = await response.json();

        // Remove temp message
        mutations.deleteMessage(tempMessage.id);

        // Add actual message
        mutations.addMessage(sentMessage);

        return sentMessage;
    } catch (error) {
        console.error('Error replying to message:', error);
        mutations.setState('status.error', error.message);

        // Update temporary message to show error
        const messages = state.messages.map(msg =>
            msg.id === `temp-${Date.now()}`
                ? { ...msg, error: true }
                : msg
        );
        mutations.setState('messages', messages);

        return null;
    }
}

/**
 * Add a reaction to a message
 * @param {string} messageId - Message ID
 * @param {string} emoji - Emoji to add
 */
async function addReaction(messageId, emoji) {
    try {
        const { selectedChannelId } = state;

        if (!selectedChannelId) {
            throw new Error('No channel selected');
        }

        // Send reaction request
        const response = await fetch(`/api/messages/${messageId}/channel/${selectedChannelId}/reactions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ emoji })
        });

        if (!response.ok) {
            throw new Error(`Failed to add reaction: ${response.status}`);
        }

        // Update message locally
        const message = state.messages.find(m => m.id === messageId);
        if (message) {
            const reactions = message.reactions || {};
            const reaction = reactions[emoji] || { count: 0, users: [] };

            reactions[emoji] = {
                ...reaction,
                count: reaction.count + 1,
                users: [...(reaction.users || []), state.currentUser?.id || '0'],
                selfReacted: true
            };

            mutations.updateMessage(messageId, { reactions });
        }

        return true;
    } catch (error) {
        console.error('Error adding reaction:', error);
        mutations.setState('status.error', error.message);
        return false;
    }
}

/**
 * Remove a reaction from a message
 * @param {string} messageId - Message ID
 * @param {string} emoji - Emoji to remove
 */
async function removeReaction(messageId, emoji) {
    try {
        const { selectedChannelId } = state;

        if (!selectedChannelId) {
            throw new Error('No channel selected');
        }

        // Send remove reaction request
        const response = await fetch(`/api/messages/${messageId}/channel/${selectedChannelId}/reactions/${encodeURIComponent(emoji)}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(`Failed to remove reaction: ${response.status}`);
        }

        // Update message locally
        const message = state.messages.find(m => m.id === messageId);
        if (message && message.reactions && message.reactions[emoji]) {
            const reactions = { ...message.reactions };
            const reaction = reactions[emoji];

            if (reaction.count <= 1) {
                // Remove reaction if it's the last one
                delete reactions[emoji];
            } else {
                // Decrement count and remove current user
                reactions[emoji] = {
                    ...reaction,
                    count: reaction.count - 1,
                    users: (reaction.users || []).filter(id => id !== state.currentUser?.id),
                    selfReacted: false
                };
            }

            mutations.updateMessage(messageId, { reactions });
        }

        return true;
    } catch (error) {
        console.error('Error removing reaction:', error);
        mutations.setState('status.error', error.message);
        return false;
    }
}

/**
 * Send typing indicator
 */
async function sendTypingIndicator() {
    try {
        const { selectedChannelId, selectedDmUserId, isDmView } = state;

        if (!window.wsHandler) {
            return false;
        }

        if (isDmView && selectedDmUserId) {
            // Send typing indicator for DM
            window.wsHandler.send({
                type: 'TYPING_START',
                data: { userId: selectedDmUserId }
            });
        } else if (selectedChannelId) {
            // Send typing indicator for channel
            window.wsHandler.send({
                type: 'TYPING_START',
                data: { channelId: selectedChannelId }
            });
        }

        return true;
    } catch (error) {
        console.error('Error sending typing indicator:', error);
        return false;
    }
}

// Export functions
export {
    initializeWebSocket,
    loadCurrentUser,
    loadGuilds,
    loadChannels,
    loadMessages,
    loadGuildMembers,
    loadDmUsers,
    loadDmMessages,
    selectGuild,
    selectChannel,
    selectDmUser,
    switchToDmView,
    sendMessage,
    editMessage,
    deleteMessage,
    replyToMessage,
    addReaction,
    removeReaction,
    sendTypingIndicator
};

// For backwards compatibility with non-module code
window.actions = {
    initializeWebSocket,
    loadCurrentUser,
    loadGuilds,
    loadChannels,
    loadMessages,
    loadGuildMembers,
    loadDmUsers,
    loadDmMessages,
    selectGuild,
    selectChannel,
    selectDmUser,
    switchToDmView,
    sendMessage,
    editMessage,
    deleteMessage,
    replyToMessage,
    addReaction,
    removeReaction,
    sendTypingIndicator
};