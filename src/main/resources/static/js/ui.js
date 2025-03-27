/**
 * UI Controller for Discord Bot Control Panel
 * Handles UI rendering and user interactions
 */

import { state } from './state.js';
import * as mutations from './mutations.js';
import * as actions from './actions.js';

// DOM element references
let messagesContainer,
    channelsList,
    usersList,
    serversList,
    messageForm,
    messageInput,
    channelHeader,
    guildHeader;

// Initialize the UI controller
function init() {
    // Get DOM references
    messagesContainer = document.getElementById('messages-container');
    channelsList = document.getElementById('channels-list');
    usersList = document.getElementById('users-container');
    serversList = document.getElementById('servers-container');
    messageForm = document.getElementById('message-form');
    messageInput = document.getElementById('message-input');
    channelHeader = document.getElementById('channel-info');
    guildHeader = document.getElementById('guild-name');

    // Set up event listeners
    setupEventListeners();

    // Subscribe to state changes
    subscribeToStateChanges();

    // Initialize WebSocket
    initializeWebSocket();

    console.log('UI initialized');
}

/**
 * Setup event listeners for UI interactions
 */
function setupEventListeners() {
    // Message form submission
    if (messageForm) {
        messageForm.addEventListener('submit', handleMessageSubmit);
    }

    // Message input typing indicator
    if (messageInput) {
        messageInput.addEventListener('input', handleTypingIndicator);
        messageInput.addEventListener('keydown', handleInputKeydown);
    }

    // Server list click handling
    if (serversList) {
        serversList.addEventListener('click', handleServerClick);
    }

    // Home/DM button
    const homeButton = document.querySelector('.server-item.home');
    if (homeButton) {
        homeButton.addEventListener('click', handleHomeClick);
    }

    // Message context menu
    if (messagesContainer) {
        messagesContainer.addEventListener('contextmenu', handleMessageContextMenu);

        // Message click for actions
        messagesContainer.addEventListener('click', handleMessageClick);

        // Scroll for loading more messages
        messagesContainer.addEventListener('scroll', handleMessagesScroll);
    }

    // Document-level click handler for closing menus
    document.addEventListener('click', handleDocumentClick);

    // Document-level keyboard shortcuts
    document.addEventListener('keydown', handleKeyboardShortcuts);
}

/**
 * Subscribe to state changes to update UI
 */
function subscribeToStateChanges() {
    // Subscribe to guilds changes
    mutations.subscribe('guilds', renderServers);

    // Subscribe to channels changes
    mutations.subscribe('channels', renderChannels);

    // Subscribe to users changes
    mutations.subscribe('users', renderUsers);

    // Subscribe to messages changes
    mutations.subscribe('messages', renderMessages);

    // Subscribe to DM users changes
    mutations.subscribe('dmUsers', renderDmList);

    // Subscribe to current user changes
    mutations.subscribe('currentUser', updateBotInfo);

    // Subscribe to typing users changes
    mutations.subscribe('typingUsers', renderTypingIndicator);

    // Subscribe to status changes
    mutations.subscribe('status', updateStatusIndicator);

    // Subscribe to selection changes
    mutations.subscribe('selectedGuildId', updateGuildHeader);
    mutations.subscribe('selectedChannelId', updateChannelHeader);
    mutations.subscribe('selectedDmUserId', updateDmHeader);
}

/**
 * Initialize WebSocket connection
 */
function initializeWebSocket() {
    if (window.wsHandler) {
        const wsEndpoint = window.CONFIG?.WEBSOCKET?.ENDPOINT || '/ws';
        actions.initializeWebSocket(wsEndpoint);
    }
}

/**
 * Handle message form submission
 * @param {Event} event - Submit event
 */
async function handleMessageSubmit(event) {
    event.preventDefault();

    const content = messageInput.value.trim();
    if (!content) return;

    // Clear input immediately for better UX
    messageInput.value = '';

    // Send message through actions
    await actions.sendMessage(content);

    // Focus input field again
    messageInput.focus();
}

/**
 * Handle typing indicator
 */
function handleTypingIndicator() {
    // Throttle typing indicator to once per 5 seconds
    if (!window.lastTypingIndicator || Date.now() - window.lastTypingIndicator > 5000) {
        window.lastTypingIndicator = Date.now();
        actions.sendTypingIndicator();
    }
}

/**
 * Handle keyboard shortcuts in message input
 * @param {KeyboardEvent} event - Keydown event
 */
function handleInputKeydown(event) {
    // Handle Escape key to cancel reply/edit
    if (event.key === 'Escape') {
        const replyBar = document.querySelector('.reply-bar');
        if (replyBar) {
            replyBar.remove();
            state.replyingTo = null;
        }
    }

    // Handle Up arrow key to edit last message
    if (event.key === 'ArrowUp' && messageInput.value === '') {
        const messages = state.messages;

        // Find last message from current user
        const lastOwnMessage = [...messages]
            .reverse()
            .find(msg => msg.authorId === state.currentUser?.id);

        if (lastOwnMessage) {
            editMessage(lastOwnMessage.id, lastOwnMessage);
        }
    }
}

/**
 * Handle server clicks
 * @param {Event} event - Click event
 */
function handleServerClick(event) {
    const serverItem = event.target.closest('.server-item');
    if (!serverItem || serverItem.classList.contains('home')) return;

    const guildId = serverItem.dataset.id;
    if (guildId) {
        actions.selectGuild(guildId);
    }
}

/**
 * Handle home/DM button click
 */
function handleHomeClick() {
    actions.switchToDmView();
}

/**
 * Handle message context menu
 * @param {Event} event - ContextMenu event
 */
function handleMessageContextMenu(event) {
    const messageItem = event.target.closest('.message-item');
    if (!messageItem) return;

    event.preventDefault();

    const messageId = messageItem.dataset.id;
    const message = state.messages.find(m => m.id === messageId);
    if (!message) return;

    // Remove any existing context menus
    removeContextMenus();

    // Create context menu
    const contextMenu = document.createElement('div');
    contextMenu.className = 'context-menu';
    contextMenu.style.top = `${event.clientY}px`;
    contextMenu.style.left = `${event.clientX}px`;

    // Add menu items based on message ownership
    const isOwnMessage = message.authorId === state.currentUser?.id;

    // Reply option for all messages
    const replyItem = createMenuItem('reply', 'Reply', 'fa-reply');
    replyItem.dataset.messageId = messageId;
    contextMenu.appendChild(replyItem);

    // Edit and delete options only for own messages
    if (isOwnMessage) {
        const editItem = createMenuItem('edit', 'Edit', 'fa-edit');
        editItem.dataset.messageId = messageId;
        contextMenu.appendChild(editItem);

        const deleteItem = createMenuItem('delete', 'Delete', 'fa-trash');
        deleteItem.dataset.messageId = messageId;
        contextMenu.appendChild(deleteItem);
    }

    // Add reaction option for all messages
    const addReactionItem = createMenuItem('add-reaction', 'Add Reaction', 'fa-smile');
    addReactionItem.dataset.messageId = messageId;
    contextMenu.appendChild(addReactionItem);

    // Copy options for all messages
    const copyTextItem = createMenuItem('copy-text', 'Copy Text', 'fa-copy');
    copyTextItem.dataset.messageId = messageId;
    contextMenu.appendChild(copyTextItem);

    const copyIdItem = createMenuItem('copy-id', 'Copy ID', 'fa-id-badge');
    copyIdItem.dataset.messageId = messageId;
    contextMenu.appendChild(copyIdItem);

    // Add context menu to document
    document.body.appendChild(contextMenu);

    // Adjust position if menu is outside viewport
    const menuRect = contextMenu.getBoundingClientRect();

    if (menuRect.right > window.innerWidth) {
        contextMenu.style.left = `${window.innerWidth - menuRect.width - 5}px`;
    }

    if (menuRect.bottom > window.innerHeight) {
        contextMenu.style.top = `${window.innerHeight - menuRect.height - 5}px`;
    }

    // Add click events to menu items
    contextMenu.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', handleContextMenuAction);
    });
}

/**
 * Create a context menu item
 * @param {string} action - Action name
 * @param {string} text - Menu text
 * @param {string} icon - FontAwesome icon class
 * @returns {HTMLElement} - Menu item element
 */
function createMenuItem(action, text, icon) {
    const item = document.createElement('div');
    item.className = 'menu-item';
    item.dataset.action = action;

    item.innerHTML = `
        <i class="fas ${icon}"></i>
        <span>${text}</span>
    `;

    return item;
}

/**
 * Handle context menu item actions
 * @param {Event} event - Click event
 */
function handleContextMenuAction(event) {
    const item = event.currentTarget;
    const action = item.dataset.action;
    const messageId = item.dataset.messageId;

    // Find message in state
    const message = state.messages.find(m => m.id === messageId);
    if (!message) return;

    // Handle different actions
    switch (action) {
        case 'reply':
            replyToMessage(messageId, message);
            break;

        case 'edit':
            editMessage(messageId, message);
            break;

        case 'delete':
            deleteMessage(messageId);
            break;

        case 'add-reaction':
            promptForReaction(messageId);
            break;

        case 'copy-text':
            copyToClipboard(message.content);
            showToast('Message copied to clipboard', 'success');
            break;

        case 'copy-id':
            copyToClipboard(messageId);
            showToast('Message ID copied to clipboard', 'info');
            break;
    }

    // Remove context menu
    removeContextMenus();
}

/**
 * Handle regular clicks on messages
 * @param {Event} event - Click event
 */
function handleMessageClick(event) {
    // Check for reaction clicks
    const reaction = event.target.closest('.message-reaction');
    if (reaction) {
        const messageId = reaction.closest('.message-item').dataset.id;
        const emoji = reaction.dataset.emoji;

        if (reaction.classList.contains('self-reacted')) {
            // Remove own reaction
            actions.removeReaction(messageId, emoji);
        } else {
            // Add reaction
            actions.addReaction(messageId, emoji);
        }

        return;
    }

    // Check for message link clicks
    const messageLink = event.target.closest('.message-reference');
    if (messageLink) {
        const referencedId = messageLink.dataset.messageId;
        scrollToMessage(referencedId);
        return;
    }
}

/**
 * Scroll to a specific message
 * @param {string} messageId - Message ID to scroll to
 */
function scrollToMessage(messageId) {
    const messageEl = document.querySelector(`.message-item[data-id="${messageId}"]`);
    if (!messageEl) return;

    // Scroll into view with smooth animation
    messageEl.scrollIntoView({ behavior: 'smooth', block: 'center' });

    // Highlight the message temporarily
    messageEl.classList.add('highlighted');
    setTimeout(() => {
        messageEl.classList.remove('highlighted');
    }, 2000);
}

/**
 * Handle messages container scroll
 * @param {Event} event - Scroll event
 */
function handleMessagesScroll(event) {
    // Load more messages when scrolled near top
    if (messagesContainer.scrollTop < 100) {
        const messages = state.messages;
        if (messages.length === 0) return;

        // Get oldest message ID
        const oldestMessageId = messages[0].id;

        // Load older messages
        if (!window.loadingMoreMessages) {
            window.loadingMoreMessages = true;

            const channelId = state.selectedChannelId;
            if (channelId) {
                actions.loadMessages(channelId, { before: oldestMessageId })
                    .finally(() => {
                        window.loadingMoreMessages = false;
                    });
            }
        }
    }
}

/**
 * Handle document-wide clicks (for closing menus, etc.)
 * @param {Event} event - Click event
 */
function handleDocumentClick(event) {
    // Close context menus if clicking outside
    if (!event.target.closest('.context-menu')) {
        removeContextMenus();
    }

    // Close emoji picker if clicking outside
    const emojiPicker = document.querySelector('.emoji-picker');
    if (emojiPicker && !event.target.closest('.emoji-picker') && !event.target.closest('.menu-item[data-action="add-reaction"]')) {
        emojiPicker.remove();
    }
}

/**
 * Handle keyboard shortcuts
 * @param {KeyboardEvent} event - Keydown event
 */
function handleKeyboardShortcuts(event) {
    // Escape key for closing UI elements
    if (event.key === 'Escape') {
        removeContextMenus();

        const emojiPicker = document.querySelector('.emoji-picker');
        if (emojiPicker) {
            emojiPicker.remove();
        }
    }

    // Alt+Up/Down for navigating channels
    if (event.altKey) {
        if (event.key === 'ArrowUp') {
            navigateToPreviousChannel();
            event.preventDefault();
        } else if (event.key === 'ArrowDown') {
            navigateToNextChannel();
            event.preventDefault();
        }
    }
}

/**
 * Navigate to previous channel in list
 */
function navigateToPreviousChannel() {
    const channelItems = Array.from(document.querySelectorAll('.channel-item, .dm-item'));
    const currentIndex = channelItems.findIndex(item => item.classList.contains('active'));

    if (currentIndex > 0) {
        const prevItem = channelItems[currentIndex - 1];
        const isChannel = prevItem.classList.contains('channel-item');
        const id = prevItem.dataset.id;

        if (isChannel) {
            actions.selectChannel(id);
        } else {
            actions.selectDmUser(id);
        }
    }
}

/**
 * Navigate to next channel in list
 */
function navigateToNextChannel() {
    const channelItems = Array.from(document.querySelectorAll('.channel-item, .dm-item'));
    const currentIndex = channelItems.findIndex(item => item.classList.contains('active'));

    if (currentIndex >= 0 && currentIndex < channelItems.length - 1) {
        const nextItem = channelItems[currentIndex + 1];
        const isChannel = nextItem.classList.contains('channel-item');
        const id = nextItem.dataset.id;

        if (isChannel) {
            actions.selectChannel(id);
        } else {
            actions.selectDmUser(id);
        }
    }
}

/**
 * Reply to a message
 * @param {string} messageId - Message ID
 * @param {object} message - Message object
 */
function replyToMessage(messageId, message) {
    // Remove existing reply bar
    const existingReplyBar = document.querySelector('.reply-bar');
    if (existingReplyBar) {
        existingReplyBar.remove();
    }

    // Create reply bar
    const replyBar = document.createElement('div');
    replyBar.className = 'reply-bar';

    replyBar.innerHTML = `
        <div class="reply-info">
            <span class="reply-icon"><i class="fas fa-reply"></i></span>
            <span>Replying to <strong>${message.authorName}</strong></span>
            <div class="reply-preview">${message.content.substring(0, 50)}${message.content.length > 50 ? '...' : ''}</div>
        </div>
        <button class="cancel-reply" type="button">
            <i class="fas fa-times"></i>
        </button>
    `;

    // Add to DOM
    messageForm.parentNode.insertBefore(replyBar, messageForm);

    // Set up cancel button
    replyBar.querySelector('.cancel-reply').addEventListener('click', () => {
        replyBar.remove();
        delete state.replyingTo;
    });

    // Set state for the reply
    state.replyingTo = messageId;

    // Focus message input
    messageInput.focus();
}

/**
 * Edit a message
 * @param {string} messageId - Message ID
 * @param {object} message - Message object
 */
function editMessage(messageId, message) {
    const messageEl = document.querySelector(`.message-item[data-id="${messageId}"]`);
    if (!messageEl) return;

    const messageTextEl = messageEl.querySelector('.message-text');
    if (!messageTextEl) return;

    // Save original content and replace with editor
    const originalContent = message.content;

    messageTextEl.innerHTML = `
        <div class="edit-container">
            <textarea class="edit-input">${originalContent}</textarea>
            <div class="edit-actions">
                <button type="button" class="cancel-edit">Cancel</button>
                <button type="button" class="save-edit">Save</button>
            </div>
            <div class="edit-hint">
                escape to <span class="edit-action">cancel</span> • enter to <span class="edit-action">save</span>
            </div>
        </div>
    `;

    // Focus the input
    const editInput = messageTextEl.querySelector('.edit-input');
    editInput.focus();
    editInput.setSelectionRange(editInput.value.length, editInput.value.length);

    // Set up event handlers
    const cancelEdit = () => {
        messageTextEl.innerHTML = formatMessageContent(originalContent);
    };

    const saveEdit = async () => {
        const newContent = editInput.value.trim();
        if (!newContent || newContent === originalContent) {
            cancelEdit();
            return;
        }

        messageTextEl.innerHTML = `<div class="loading-edit">Saving edit...</div>`;

        try {
            await actions.editMessage(messageId, newContent);
        } catch (error) {
            console.error('Error saving edit:', error);
            messageTextEl.innerHTML = formatMessageContent(originalContent);
            showToast('Failed to save edit', 'error');
        }
    };

    // Set up button handlers
    messageTextEl.querySelector('.cancel-edit').addEventListener('click', cancelEdit);
    messageTextEl.querySelector('.save-edit').addEventListener('click', saveEdit);

    // Set up keyboard shortcuts
    editInput.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            e.preventDefault();
            cancelEdit();
        } else if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            saveEdit();
        }
    });
}

/**
 * Delete a message
 * @param {string} messageId - Message ID
 */
async function deleteMessage(messageId) {
    // Confirm deletion
    if (!confirm('Are you sure you want to delete this message? This cannot be undone.')) {
        return;
    }

    try {
        await actions.deleteMessage(messageId);
        showToast('Message deleted', 'success');
    } catch (error) {
        console.error('Error deleting message:', error);
        showToast('Failed to delete message', 'error');
    }
}

/**
 * Prompt for reaction emoji
 * @param {string} messageId - Message ID
 */
function promptForReaction(messageId) {
    // Remove existing emoji picker
    const existingPicker = document.querySelector('.emoji-picker');
    if (existingPicker) {
        existingPicker.remove();
    }

    // Create a simple emoji picker
    const emojiPicker = document.createElement('div');
    emojiPicker.className = 'emoji-picker';

    // Common emojis
    const commonEmojis = ['👍', '👎', '❤️', '😄', '😢', '🎉', '🔥', '🤔', '👀', '🙏'];

    // Create emoji picker content
    let emojiContent = `<div class="emoji-picker-header">Select an emoji</div>`;
    emojiContent += `<div class="emoji-grid">`;

    commonEmojis.forEach(emoji => {
        emojiContent += `<div class="emoji-item" data-emoji="${emoji}">${emoji}</div>`;
    });

    emojiContent += `</div>`;
    emojiContent += `<div class="emoji-picker-input">
        <input type="text" placeholder="Custom emoji" maxlength="2">
        <button type="button">Add</button>
    </div>`;

    emojiPicker.innerHTML = emojiContent;

    // Position the picker
    const messageEl = document.querySelector(`.message-item[data-id="${messageId}"]`);
    if (messageEl) {
        const rect = messageEl.getBoundingClientRect();
        emojiPicker.style.top = `${rect.bottom + 10}px`;
        emojiPicker.style.left = `${rect.left + 20}px`;
    } else {
        // Default position if message element not found
        emojiPicker.style.top = '50%';
        emojiPicker.style.left = '50%';
        emojiPicker.style.transform = 'translate(-50%, -50%)';
    }

    // Add to DOM
    document.body.appendChild(emojiPicker);

    // Add event listeners
    emojiPicker.querySelectorAll('.emoji-item').forEach(item => {
        item.addEventListener('click', () => {
            const emoji = item.dataset.emoji;
            actions.addReaction(messageId, emoji);
            emojiPicker.remove();
        });
    });

    // Custom emoji input
    const emojiInput = emojiPicker.querySelector('input');
    const addButton = emojiPicker.querySelector('button');

    addButton.addEventListener('click', () => {
        const customEmoji = emojiInput.value.trim();
        if (customEmoji) {
            actions.addReaction(messageId, customEmoji);
            emojiPicker.remove();
        }
    });

    emojiInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            const customEmoji = emojiInput.value.trim();
            if (customEmoji) {
                actions.addReaction(messageId, customEmoji);
                emojiPicker.remove();
            }
        }
    });

    // Focus the input
    emojiInput.focus();
}

/**
 * Copy text to clipboard
 * @param {string} text - Text to copy
 */
function copyToClipboard(text) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = 0;

    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
}

/**
 * Remove all context menus
 */
function removeContextMenus() {
    document.querySelectorAll('.context-menu').forEach(menu => {
        menu.remove();
    });
}

/**
 * Show a toast notification
 * @param {string} message - Toast message
 * @param {string} type - Toast type (success, error, info)
 */
function showToast(message, type = 'info') {
    // Remove existing toasts
    document.querySelectorAll('.toast').forEach(toast => {
        toast.remove();
    });

    // Create toast
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon;
    switch (type) {
        case 'success':
            icon = 'fa-check-circle';
            break;
        case 'error':
            icon = 'fa-exclamation-circle';
            break;
        default:
            icon = 'fa-info-circle';
    }

    toast.innerHTML = `
        <div class="toast-content">
            <i class="fas ${icon}"></i>
            <span>${message}</span>
        </div>
    `;

    // Add to DOM
    document.body.appendChild(toast);

    // Animate in
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    // Remove after delay
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

/**
 * Format message content with markdown-like parsing
 * @param {string} content - Message content
 * @returns {string} - Formatted HTML
 */
function formatMessageContent(content) {
    if (!content) return '';

    // Escape HTML
    let formatted = content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

    // Format code blocks
    formatted = formatted.replace(/```(\w+)?\n([\s\S]*?)\n```/g, (match, language, code) => {
        const lang = language ? ` class="language-${language}"` : '';
        return `<pre><code${lang}>${code}</code></pre>`;
    });

    // Format inline code
    formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Format bold text
    formatted = formatted.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

    // Format italic text
    formatted = formatted.replace(/\*(.+?)\*/g, '<em>$1</em>');

    // Format underline text
    formatted = formatted.replace(/\_\_(.+?)\_\_/g, '<u>$1</u>');

    // Format strikethrough text
    formatted = formatted.replace(/\~\~(.+?)\~\~/g, '<s>$1</s>');

    // Format mentions
    formatted = formatted.replace(/@(\w+)/g, '<span class="mention">@$1</span>');

    // Format channel mentions
    formatted = formatted.replace(/#(\w+)/g, '<span class="channel-mention">#$1</span>');

    // Convert URLs to links
    formatted = formatted.replace(/(https?:\/\/[^\s]+)/g, '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>');

    // Replace newlines with <br>
    formatted = formatted.replace(/\n/g, '<br>');

    return formatted;
}

/**
 * Render the server list
 */
function renderServers() {
    if (!serversList) return;

    const guilds = state.guilds;

    // Clear current content
    serversList.innerHTML = '';

    // Add each server
    guilds.forEach(guild => {
        const serverEl = document.createElement('div');
        serverEl.className = `server-item${state.selectedGuildId === guild.id ? ' active' : ''}`;
        serverEl.dataset.id = guild.id;

        let iconContent;
        if (guild.iconUrl) {
            iconContent = `<img src="${guild.iconUrl}" alt="${guild.name}">`;
        } else {
            // Create initials from guild name
            const initials = guild.name
                .split(' ')
                .map(word => word.charAt(0))
                .join('')
                .substring(0, 2)
                .toUpperCase();
            iconContent = initials;
        }

        serverEl.innerHTML = `
            <div class="server-icon">${iconContent}</div>
            <div class="server-tooltip">${guild.name}</div>
        `;

        serversList.appendChild(serverEl);
    });
}

/**
 * Render the channel list
 */
function renderChannels() {
    if (!channelsList) return;

    const channels = state.channels;

    // Clear current content
    channelsList.innerHTML = '';

    // Group channels by category
    const categories = {};
    const uncategorizedChannels = [];

    channels.forEach(channel => {
        if (channel.type === 'CATEGORY') {
            categories[channel.id] = {
                ...channel,
                channels: []
            };
        } else if (channel.categoryId) {
            if (categories[channel.categoryId]) {
                categories[channel.categoryId].channels.push(channel);
            } else {
                uncategorizedChannels.push(channel);
            }
        } else {
            uncategorizedChannels.push(channel);
        }
    });

    // Add uncategorized channels first
    uncategorizedChannels
        .filter(channel => channel.type === 'TEXT' || channel.type === 'VOICE')
        .forEach(channel => {
            channelsList.appendChild(createChannelElement(channel));
        });

    // Then add categories and their channels
    Object.values(categories).forEach(category => {
        // Create category element
        const categoryEl = document.createElement('div');
        categoryEl.className = 'channel-category';

        categoryEl.innerHTML = `
            <div class="category-header">
                <span class="category-arrow">▼</span>
                <span class="category-name">${category.name}</span>
            </div>
        `;

        channelsList.appendChild(categoryEl);

        // Create container for category's channels
        const channelsContainerEl = document.createElement('div');
        channelsContainerEl.className = 'category-channels';

        // Sort channels by position
        category.channels
            .filter(channel => channel.type === 'TEXT' || channel.type === 'VOICE')
            .sort((a, b) => (a.position || 0) - (b.position || 0))
            .forEach(channel => {
                channelsContainerEl.appendChild(createChannelElement(channel));
            });

        channelsList.appendChild(channelsContainerEl);

        // Add expand/collapse functionality
        // Add expand/collapse functionality
        const categoryHeader = categoryEl.querySelector('.category-header');
        categoryHeader.addEventListener('click', () => {
            categoryEl.classList.toggle('collapsed');
            channelsContainerEl.style.display = categoryEl.classList.contains('collapsed') ? 'none' : 'block';

            // Update arrow
            const arrow = categoryEl.querySelector('.category-arrow');
            arrow.textContent = categoryEl.classList.contains('collapsed') ? '▶' : '▼';
        });
    });
}

/**
 * Create a channel element
 * @param {object} channel - Channel data
 * @returns {HTMLElement} - Channel element
 */
function createChannelElement(channel) {
    const channelEl = document.createElement('div');
    channelEl.className = `channel-item${channel.type.toLowerCase()}${state.selectedChannelId === channel.id ? ' active' : ''}`;
    channelEl.dataset.id = channel.id;

    // Choose icon based on channel type
    let icon = '';
    if (channel.type === 'TEXT') {
        icon = '<span class="channel-icon"><i class="fas fa-hashtag"></i></span>';
    } else if (channel.type === 'VOICE') {
        icon = '<span class="channel-icon"><i class="fas fa-volume-up"></i></span>';
    }

    channelEl.innerHTML = `
        ${icon}
        <span class="channel-name">${channel.name}</span>
    `;

    // Add click handler
    channelEl.addEventListener('click', () => {
        actions.selectChannel(channel.id);
    });

    return channelEl;
}

/**
 * Render the DM user list
 */
function renderDmList() {
    if (!channelsList) return;

    const dmUsers = state.dmUsers;

    // Clear current content
    channelsList.innerHTML = '';

    // Add each DM user
    dmUsers.forEach(user => {
        const dmEl = document.createElement('div');
        dmEl.className = `dm-item${state.selectedDmUserId === user.id ? ' active' : ''}`;
        dmEl.dataset.id = user.id;

        // Add special classes
        if (user.isOwner) {
            dmEl.classList.add('owner');
        }
        if (user.bot) {
            dmEl.classList.add('bot');
        }

        let avatar = '';
        if (user.avatarUrl) {
            avatar = `<img src="${user.avatarUrl}" alt="${user.name}">`;
        } else {
            // Use first letter of display name as avatar
            const displayName = user.displayName || user.name;
            const avatarText = displayName.charAt(0).toUpperCase();
            avatar = `<div class="avatar-text">${avatarText}</div>`;
        }

        // Get status color
        const statusClass = user.status ? user.status.toLowerCase() : 'offline';

        // Add badges
        let badges = '';

        // Owner badge (crown icon)
        if (user.isOwner) {
            badges += ' <span class="owner-badge"><i class="fas fa-crown"></i></span>';
        }

        // Bot badge (robot icon)
        if (user.bot) {
            badges += ' <span class="bot-badge"><i class="fas fa-robot"></i></span>';
        }

        dmEl.innerHTML = `
            <div class="dm-avatar">
                ${avatar}
                <div class="dm-status ${statusClass}"></div>
            </div>
            <span class="dm-name">${user.displayName || user.name}${badges}</span>
        `;

        // Add click handler
        dmEl.addEventListener('click', () => {
            if (user.bot) {
                showToast('Cannot message bots directly', 'error');
            } else {
                actions.selectDmUser(user.id);
            }
        });

        channelsList.appendChild(dmEl);
    });
}

/**
 * Render the user list
 */
function renderUsers() {
    if (!usersList) return;

    const users = state.users;

    // Clear current content
    usersList.innerHTML = '';

    // Group users by status
    const groups = {
        online: [],
        idle: [],
        dnd: [],
        offline: []
    };

    users.forEach(user => {
        const status = user.status || 'offline';
        if (groups[status]) {
            groups[status].push(user);
        } else {
            groups.offline.push(user);
        }
    });

    // Sort users by name within each group
    Object.keys(groups).forEach(status => {
        groups[status].sort((a, b) => {
            const nameA = a.displayName || a.name || '';
            const nameB = b.displayName || b.name || '';
            return nameA.localeCompare(nameB);
        });
    });

    // Status group names
    const statusNames = {
        online: 'ONLINE',
        idle: 'IDLE',
        dnd: 'DO NOT DISTURB',
        offline: 'OFFLINE'
    };

    // Add each status group with its users
    Object.keys(groups).forEach(status => {
        const users = groups[status];
        if (users.length === 0) return;

        // Create group element
        const groupEl = document.createElement('div');
        groupEl.className = 'user-group';

        groupEl.innerHTML = `
            <div class="user-group-header">
                ${statusNames[status]} - ${users.length}
            </div>
        `;

        // Add each user to the group
        users.forEach(user => {
            const userEl = document.createElement('div');
            userEl.className = 'user-item';
            userEl.dataset.id = user.id;

            let avatar = '';
            if (user.avatarUrl) {
                avatar = `<img src="${user.avatarUrl}" alt="${user.displayName || user.name}">`;
            } else {
                // Use first letter of display name as avatar
                const displayName = user.displayName || user.name;
                const avatarText = displayName.charAt(0).toUpperCase();
                avatar = `<div class="avatar-text">${avatarText}</div>`;
            }

            // Add badges or indicators
            let badges = '';

            // Bot badge
            if (user.bot) {
                badges += ' <span class="bot-badge"><i class="fas fa-robot"></i></span>';
            }

            // Owner badge
            if (user.id === state.guilds.find(g => g.id === state.selectedGuildId)?.ownerId) {
                badges += ' <span class="owner-badge"><i class="fas fa-crown"></i></span>';
            }

            userEl.innerHTML = `
                <div class="user-avatar">
                    ${avatar}
                    <div class="user-status ${status}"></div>
                </div>
                <span class="user-name">${user.displayName || user.name}${badges}</span>
            `;

            // Add click handler to start DM
            userEl.addEventListener('click', () => {
                if (user.bot) {
                    showToast('Cannot message bots directly', 'error');
                } else {
                    actions.selectDmUser(user.id);
                }
            });

            groupEl.appendChild(userEl);
        });

        usersList.appendChild(groupEl);
    });
}

/**
 * Render messages
 */
function renderMessages() {
    if (!messagesContainer) return;

    const messages = state.messages;

    // Save scroll position
    const wasAtBottom = isAtBottom();
    const scrollPosition = messagesContainer.scrollTop;
    const oldScrollHeight = messagesContainer.scrollHeight;

    // Store typing indicator to restore it later
    const typingIndicator = messagesContainer.querySelector('.typing-indicator');
    if (typingIndicator) {
        typingIndicator.remove();
    }

    // Clear container if no messages
    if (messages.length === 0) {
        messagesContainer.innerHTML = `
            <div class="welcome-message">
                <h2>No messages yet</h2>
                <p>Send a message to start the conversation!</p>
            </div>
        `;
        return;
    }

    // Otherwise, render messages
    messagesContainer.innerHTML = '';

    // Sort messages by timestamp
    const sortedMessages = [...messages].sort((a, b) => a.timestamp - b.timestamp);

    // Group messages by author for continuation
    let lastAuthorId = null;
    let lastTimestamp = null;
    const timestampThreshold = 5 * 60 * 1000; // 5 minutes

    sortedMessages.forEach((message, index) => {
        // Check if this is a continuation message
        const timeDiff = lastTimestamp ? message.timestamp - lastTimestamp : Infinity;
        const isContinuation = message.authorId === lastAuthorId && timeDiff < timestampThreshold;

        // Create message element
        const messageEl = createMessageElement(message, isContinuation);
        messagesContainer.appendChild(messageEl);

        // Update tracking variables
        lastAuthorId = message.authorId;
        lastTimestamp = message.timestamp;
    });

    // Restore typing indicator
    if (typingIndicator) {
        messagesContainer.appendChild(typingIndicator);
    }

    // Maintain scroll position
    if (wasAtBottom) {
        scrollToBottom();
    } else {
        // Try to maintain relative scroll position
        messagesContainer.scrollTop = scrollPosition + (messagesContainer.scrollHeight - oldScrollHeight);
    }
}

/**
 * Create a message element
 * @param {object} message - Message data
 * @param {boolean} isContinuation - Whether this is a continuation message
 * @returns {HTMLElement} - Message element
 */
function createMessageElement(message, isContinuation) {
    const messageEl = document.createElement('div');
    messageEl.className = `message-item${isContinuation ? ' continuation' : ''}`;
    messageEl.dataset.id = message.id;

    // Add special classes
    if (message.authorId === state.currentUser?.id) {
        messageEl.classList.add('own-message');
    }

    if (message.pending) {
        messageEl.classList.add('pending');
    }

    if (message.error) {
        messageEl.classList.add('error');
    }

    if (message.system) {
        messageEl.classList.add('system-message');
        messageEl.innerHTML = `
            <div class="system-message-content">
                ${message.content}
            </div>
        `;
        return messageEl;
    }

    // Prepare message content
    let content = '';

    // Avatar and author for non-continuation messages
    if (!isContinuation) {
        let avatar = '';
        if (message.authorAvatarUrl) {
            avatar = `<img src="${message.authorAvatarUrl}" alt="${message.authorName}">`;
        } else {
            // Use first letter of author name as avatar
            const avatarText = message.authorName.charAt(0).toUpperCase();
            avatar = `<div class="avatar-text">${avatarText}</div>`;
        }

        // Format timestamp
        const date = new Date(message.timestamp);
        const timestamp = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        content += `
            <div class="message-avatar">
                ${avatar}
            </div>
            <div class="message-content">
                <div class="message-author">
                    <span class="author-name" style="color: ${getAuthorColor(message.authorId)}">
                        ${message.authorName}
                    </span>
                    <span class="message-timestamp" title="${date.toLocaleString()}">
                        ${timestamp}
                    </span>
                    ${message.edited ? '<span class="edited-indicator">(edited)</span>' : ''}
                </div>
        `;
    } else {
        // Continuation message has placeholder avatar and timestamp on hover
        const date = new Date(message.timestamp);
        const timestamp = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        content += `
            <div class="message-avatar-placeholder"></div>
            <div class="message-content">
                <div class="hover-timestamp">
                    <span class="message-timestamp" title="${date.toLocaleString()}">
                        ${timestamp}
                    </span>
                    ${message.edited ? '<span class="edited-indicator">(edited)</span>' : ''}
                </div>
        `;
    }

    // Message content - with reference if replying
    if (message.referencedMessageId) {
        const referencedMessage = state.messages.find(m => m.id === message.referencedMessageId);
        if (referencedMessage) {
            content += `
                <div class="message-reference" data-message-id="${message.referencedMessageId}">
                    <i class="fas fa-reply"></i>
                    <span>Replying to <strong>${referencedMessage.authorName}</strong></span>
                </div>
            `;
        }
    }

    // Message text
    content += `
        <div class="message-text">
            ${formatMessageContent(message.content)}
        </div>
    `;

    // Attachments
    if (message.attachments && message.attachments.length > 0) {
        content += `<div class="message-attachments">`;

        message.attachments.forEach(attachment => {
            const isImage = attachment.contentType && attachment.contentType.startsWith('image/');

            if (isImage) {
                content += `
                    <div class="attachment">
                        <img src="${attachment.url}" alt="${attachment.fileName}" />
                    </div>
                `;
            } else {
                // Determine file icon
                let fileIcon = 'fa-file';
                if (attachment.contentType) {
                    if (attachment.contentType.includes('pdf')) fileIcon = 'fa-file-pdf';
                    else if (attachment.contentType.includes('word')) fileIcon = 'fa-file-word';
                    else if (attachment.contentType.includes('excel')) fileIcon = 'fa-file-excel';
                    else if (attachment.contentType.includes('zip')) fileIcon = 'fa-file-archive';
                    else if (attachment.contentType.includes('audio')) fileIcon = 'fa-file-audio';
                    else if (attachment.contentType.includes('video')) fileIcon = 'fa-file-video';
                    else if (attachment.contentType.includes('text')) fileIcon = 'fa-file-alt';
                    else if (attachment.contentType.includes('code')) fileIcon = 'fa-file-code';
                }

                // Format file size
                let fileSize = '';
                if (attachment.size) {
                    if (attachment.size < 1024) fileSize = `${attachment.size} B`;
                    else if (attachment.size < 1024 * 1024) fileSize = `${Math.round(attachment.size / 1024 * 10) / 10} KB`;
                    else fileSize = `${Math.round(attachment.size / (1024 * 1024) * 10) / 10} MB`;
                }

                content += `
                    <div class="file-attachment">
                        <div class="file-icon">
                            <i class="fas ${fileIcon}"></i>
                        </div>
                        <div class="file-info">
                            <div class="file-name">${attachment.fileName}</div>
                            <div class="file-size">${fileSize}</div>
                        </div>
                        <a href="${attachment.url}" target="_blank" rel="noopener noreferrer" class="download-button">
                            <i class="fas fa-download"></i>
                        </a>
                    </div>
                `;
            }
        });

        content += `</div>`;
    }

    // Embeds
    if (message.embeds && message.embeds.length > 0) {
        content += `<div class="message-embeds">`;

        message.embeds.forEach(embed => {
            // Determine border color
            const borderColor = embed.color
                ? `#${embed.color.toString(16).padStart(6, '0')}`
                : '#4f545c';

            content += `<div class="message-embed" style="border-left-color: ${borderColor}">`;

            // Author section
            if (embed.author) {
                content += `
                    <div class="embed-author">
                        ${embed.author.iconUrl ? `<img src="${embed.author.iconUrl}" alt="" />` : ''}
                        ${embed.author.url
                    ? `<a href="${embed.author.url}" target="_blank" rel="noopener noreferrer">${embed.author.name}</a>`
                    : embed.author.name}
                    </div>
                `;
            }

            // Title
            if (embed.title) {
                content += `
                    <div class="embed-title">
                        ${embed.url
                    ? `<a href="${embed.url}" target="_blank" rel="noopener noreferrer">${embed.title}</a>`
                    : embed.title}
                    </div>
                `;
            }

            // Description
            if (embed.description) {
                content += `<div class="embed-description">${formatMessageContent(embed.description)}</div>`;
            }

            // Fields
            if (embed.fields && embed.fields.length > 0) {
                content += '<div class="embed-fields">';

                embed.fields.forEach(field => {
                    content += `
                        <div class="embed-field ${field.inline ? 'inline' : ''}">
                            <div class="field-name">${field.name}</div>
                            <div class="field-value">${formatMessageContent(field.value)}</div>
                        </div>
                    `;
                });

                content += '</div>';
            }

            // Image
            if (embed.image) {
                content += `
                    <div class="embed-image">
                        <img src="${embed.image.url}" alt="" />
                    </div>
                `;
            }

            // Thumbnail
            if (embed.thumbnail) {
                content += `
                    <div class="embed-thumbnail">
                        <img src="${embed.thumbnail.url}" alt="" />
                    </div>
                `;
            }

            // Footer
            if (embed.footer) {
                content += `
                    <div class="embed-footer">
                        ${embed.footer.iconUrl ? `<img src="${embed.footer.iconUrl}" alt="" />` : ''}
                        <span>${embed.footer.text}</span>
                        ${embed.timestamp
                    ? `<span class="embed-timestamp"> • ${new Date(embed.timestamp).toLocaleString()}</span>`
                    : ''}
                    </div>
                `;
            }

            content += '</div>';
        });

        content += '</div>';
    }

    // Reactions
    if (message.reactions && Object.keys(message.reactions).length > 0) {
        content += '<div class="message-reactions">';

        Object.entries(message.reactions).forEach(([emoji, reaction]) => {
            const selfReacted = reaction.selfReacted || (reaction.users && reaction.users.includes(state.currentUser?.id));

            content += `
                <div class="message-reaction ${selfReacted ? 'self-reacted' : ''}" data-emoji="${emoji}">
                    <span class="reaction-emoji">${emoji}</span>
                    <span class="reaction-count">${reaction.count || 0}</span>
                </div>
            `;
        });

        content += '</div>';
    }

    // Close message content div
    content += '</div>';

    messageEl.innerHTML = content;
    return messageEl;
}

/**
 * Render typing indicator
 */
function renderTypingIndicator() {
    // Remove existing typing indicator
    const existingIndicator = messagesContainer.querySelector('.typing-indicator');
    if (existingIndicator) {
        existingIndicator.remove();
    }

    // If no typing users, return early
    if (state.typingUsers.size === 0) return;

    // Create typing indicator
    const typingIndicator = document.createElement('div');
    typingIndicator.className = 'typing-indicator';

    // Get typing users
    const typingUserIds = Array.from(state.typingUsers.keys());
    const typingUsers = typingUserIds.map(userId => {
        const user = findUserById(userId);
        return user ? (user.displayName || user.name) : 'Someone';
    });

    // Create typing text based on number of users
    let typingText;
    if (typingUsers.length === 1) {
        typingText = `${typingUsers[0]} is typing...`;
    } else if (typingUsers.length === 2) {
        typingText = `${typingUsers[0]} and ${typingUsers[1]} are typing...`;
    } else if (typingUsers.length === 3) {
        typingText = `${typingUsers[0]}, ${typingUsers[1]}, and ${typingUsers[2]} are typing...`;
    } else {
        typingText = 'Several people are typing...';
    }

    typingIndicator.innerHTML = `
        <div class="typing-animation">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
        </div>
        <span class="typing-text">${typingText}</span>
    `;

    // Add to messages container
    messagesContainer.appendChild(typingIndicator);

    // Scroll to bottom if we were at bottom already
    if (isAtBottom()) {
        scrollToBottom();
    }
}

/**
 * Update the guild header
 */
function updateGuildHeader() {
    if (!guildHeader) return;

    const guildId = state.selectedGuildId;

    if (guildId) {
        const guild = state.guilds.find(g => g.id === guildId);
        if (guild) {
            guildHeader.textContent = guild.name;
        }
    } else if (state.isDmView) {
        guildHeader.textContent = 'Direct Messages';
    }
}

/**
 * Update the channel header
 */
function updateChannelHeader() {
    if (!channelHeader) return;

    const channelId = state.selectedChannelId;

    if (channelId) {
        const channel = state.channels.find(c => c.id === channelId);
        if (channel) {
            let icon = '';
            if (channel.type === 'TEXT') {
                icon = '<span class="channel-icon"><i class="fas fa-hashtag"></i></span>';
            } else if (channel.type === 'VOICE') {
                icon = '<span class="channel-icon"><i class="fas fa-volume-up"></i></span>';
            }

            let html = `
                ${icon}
                <span class="channel-name">${channel.name}</span>
            `;

            if (channel.topic) {
                html += `
                    <div class="channel-topic" title="${channel.topic}">
                        ${channel.topic}
                    </div>
                `;
            }

            channelHeader.innerHTML = html;
        }
    }
}

/**
 * Update the DM header
 */
function updateDmHeader() {
    if (!channelHeader) return;

    const userId = state.selectedDmUserId;

    if (userId && state.isDmView) {
        const user = findUserById(userId);

        if (user) {
            let avatar = '';
            if (user.avatarUrl) {
                avatar = `<img src="${user.avatarUrl}" alt="${user.displayName || user.name}">`;
            } else {
                const displayName = user.displayName || user.name;
                const avatarText = displayName.charAt(0).toUpperCase();
                avatar = `<div class="avatar-text">${avatarText}</div>`;
            }

            const html = `
                <div class="user-avatar">
                    ${avatar}
                </div>
                <span class="channel-name">${user.displayName || user.name}</span>
            `;

            channelHeader.innerHTML = html;
        }
    }
}

/**
 * Update the bot info in the status bar
 */
function updateBotInfo() {
    const botUser = state.currentUser;
    if (!botUser) return;

    const botName = document.querySelector('.bot-name');
    const botDiscriminator = document.querySelector('.bot-discriminator');
    const botAvatar = document.querySelector('.bot-avatar');

    if (botName) {
        botName.textContent = botUser.name;
    }

    if (botDiscriminator) {
        botDiscriminator.textContent = botUser.discriminator ? `#${botUser.discriminator}` : '';
    }

    if (botAvatar) {
        if (botUser.avatarUrl) {
            botAvatar.innerHTML = `<img src="${botUser.avatarUrl}" alt="${botUser.name}">`;
        } else {
            const avatarText = botUser.name.charAt(0).toUpperCase();
            botAvatar.innerHTML = `<div class="default-avatar">${avatarText}</div>`;
        }
    }
}

/**
 * Update status indicator
 */
function updateStatusIndicator() {
    const status = state.status;
    const connectionStatusElement = document.getElementById('connection-status');
    const statusIndicator = document.querySelector('.status-indicator');

    if (!connectionStatusElement || !statusIndicator) return;

    if (status.connected) {
        connectionStatusElement.textContent = 'Connected';
        statusIndicator.className = 'status-indicator online';
    } else if (status.loading) {
        connectionStatusElement.textContent = 'Connecting...';
        statusIndicator.className = 'status-indicator';
    } else if (status.error) {
        connectionStatusElement.textContent = 'Connection Error';
        statusIndicator.className = 'status-indicator';
    } else {
        connectionStatusElement.textContent = 'Disconnected';
        statusIndicator.className = 'status-indicator';
    }
}

/**
 * Find a user by ID in any of the user lists
 * @param {string} userId - User ID
 * @returns {object|null} - User object or null if not found
 */
function findUserById(userId) {
    // Check main users list
    const user = state.users.find(u => u.id === userId);
    if (user) return user;

    // Check DM users list
    const dmUser = state.dmUsers.find(u => u.id === userId);
    if (dmUser) return dmUser;

    return null;
}

/**
 * Show a loading screen
 * @param {string} message - Loading message
 * @returns {HTMLElement} - Loading screen element
 */
function showLoadingScreen(message = 'Loading Discord') {
    const loadingScreen = document.createElement('div');
    loadingScreen.className = 'loading-screen';

    loadingScreen.innerHTML = `
        <div class="loading-logo">
            <i class="fa-brands fa-discord"></i>
        </div>
        <div class="loading-text">${message}</div>
        <div class="loading-bar">
            <div class="loading-progress"></div>
        </div>
    `;

    document.body.appendChild(loadingScreen);
    return loadingScreen;
}

/**
 * Hide loading screen
 * @param {HTMLElement} loadingScreen - Loading screen element
 */
function hideLoadingScreen(loadingScreen) {
    if (!loadingScreen) return;

    loadingScreen.style.opacity = '0';
    setTimeout(() => {
        if (loadingScreen.parentNode) {
            loadingScreen.parentNode.removeChild(loadingScreen);
        }
    }, 300);
}

/**
 * Show loading error
 * @param {HTMLElement} loadingScreen - Loading screen element
 * @param {Error} error - Error object
 */
function showLoadingError(loadingScreen, error) {
    if (!loadingScreen) return;

    loadingScreen.innerHTML = `
        <div class="loading-logo">
            <i class="fa-brands fa-discord"></i>
        </div>
        <div class="loading-text">Error loading Discord</div>
        <p style="color: #72767d; margin-bottom: 16px;">
            ${error.message || 'Something went wrong. Please refresh the page.'}
        </p>
        <button onclick="location.reload()" style="padding: 8px 16px; background-color: #5865F2; color: white; border: none; border-radius: 4px; cursor: pointer;">
            Refresh
        </button>
    `;
}

/**
 * Show channel loading state
 */
function showChannelLoading() {
    if (!messagesContainer) return;

    messagesContainer.innerHTML = `
        <div class="loading-container">
            <div class="loading-spinner"></div>
            <div class="loading-text">Loading messages...</div>
        </div>
    `;
}

/**
 * Get color for user name based on user ID
 * @param {string} userId - User ID
 * @returns {string} - CSS color
 */
function getAuthorColor(userId) {
    // Discord-like colors for usernames
    const colors = [
        '#1abc9c', '#2ecc71', '#3498db', '#9b59b6', '#e91e63',
        '#f1c40f', '#e67e22', '#e74c3c', '#6e48bb', '#607d8b'
    ];

    // Hash the userId to get a consistent index
    let hash = 0;
    for (let i = 0; i < userId.length; i++) {
        hash = ((hash << 5) - hash) + userId.charCodeAt(i);
        hash |= 0; // Convert to 32bit integer
    }

    // Use absolute value and modulo to get index
    const colorIndex = Math.abs(hash) % colors.length;
    return colors[colorIndex];
}

/**
 * Check if messages container is scrolled to bottom
 * @returns {boolean} - True if at bottom
 */
function isAtBottom() {
    if (!messagesContainer) return false;

    return messagesContainer.scrollHeight - messagesContainer.scrollTop <= messagesContainer.clientHeight + 50;
}

/**
 * Scroll messages container to bottom
 */
function scrollToBottom() {
    if (messagesContainer) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}

// Export UI functions
export {
    init,
    renderServers,
    renderChannels,
    renderUsers,
    renderMessages,
    renderDmList,
    updateBotInfo,
    showLoadingScreen,
    hideLoadingScreen,
    showLoadingError,
    showChannelLoading,
    showToast
};

// Initialize UI when DOM is loaded
document.addEventListener('DOMContentLoaded', init);

// Make UI functions available globally for backward compatibility
window.ui = {
    init,
    renderServers,
    renderChannels,
    renderUsers,
    renderMessages,
    renderDmList,
    updateBotInfo,
    showLoadingScreen,
    hideLoadingScreen,
    showLoadingError,
    showChannelLoading,
    showToast,

    // Additional globally available UI helper functions
    scrollToBottom,
    formatMessageContent,
    getAuthorColor,
    createChannelElement,
    createMessageElement,

    // Action helpers exposed for direct access from HTML
    handleMessageSubmit,
    handleServerClick,
    handleHomeClick,

    // State display helpers
    handleTypingIndicator,

    // User interaction handlers
    replyToMessage,
    editMessage,
    deleteMessage,
    addReaction: (messageId, emoji) => actions.addReaction(messageId, emoji),
    removeReaction: (messageId, emoji) => actions.removeReaction(messageId, emoji)
};