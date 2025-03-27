/**
 * DTO classes for Discord entities
 */

/**
 * Guild (Server) DTO
 */
class GuildDTO {
    constructor(data = {}) {
        this.id = data.id || '';
        this.name = data.name || 'Unknown Server';
        this.iconUrl = data.iconUrl || '';
        this.description = data.description || '';
        this.memberCount = data.memberCount || 0;
        this.ownerId = data.ownerId || '';
        this.features = data.features || [];
        this.textChannelCount = data.textChannelCount || 0;
        this.voiceChannelCount = data.voiceChannelCount || 0;
    }

    get initials() {
        return this.name
            .split(' ')
            .map(word => word.charAt(0))
            .join('')
            .substring(0, 2);
    }
}

/**
 * Channel DTO
 */
class ChannelDTO {
    constructor(data = {}) {
        this.id = data.id || '';
        this.name = data.name || 'unknown-channel';
        this.type = data.type || 'TEXT';
        this.guildId = data.guildId || '';
        this.categoryId = data.categoryId || null;
        this.topic = data.topic || '';
    }

    /**
     * Check if this is a text channel
     */
    isText() {
        return this.type === 'TEXT';
    }

    /**
     * Check if this is a voice channel
     */
    isVoice() {
        return this.type === 'VOICE';
    }

    /**
     * Check if this is a category
     */
    isCategory() {
        return this.type === 'CATEGORY';
    }

    /**
     * Get channel icon class
     */
    getIconClass() {
        switch (this.type) {
            case 'TEXT':
                return 'fas fa-hashtag';
            case 'VOICE':
                return 'fas fa-volume-up';
            case 'CATEGORY':
                return 'fas fa-folder';
            case 'DM':
                return 'fas fa-user';
            default:
                return 'fas fa-hashtag';
        }
    }
}

/**
 * Message DTO
 */
class MessageDTO {
    constructor(data = {}) {
        this.id = data.id || '';
        this.content = data.content || '';
        this.channelId = data.channelId || '';
        this.authorId = data.authorId || '';
        this.authorName = data.authorName || 'Unknown User';
        this.authorAvatarUrl = data.authorAvatarUrl || '';
        this.timestamp = data.timestamp || Date.now();
        this.edited = data.edited || false;
        this.editedTimestamp = data.editedTimestamp || null;
        this.attachments = data.attachments || [];
        this.embeds = data.embeds || [];
        this.reactions = data.reactions || {};
        this.referencedMessageId = data.referencedMessageId || null;
    }

    /**
     * Format message timestamp for display
     */
    getFormattedTime() {
        const date = new Date(this.timestamp);
        return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
    }

    /**
     * Format message date for display
     */
    getFormattedDate() {
        const date = new Date(this.timestamp);
        return date.toLocaleDateString();
    }

    /**
     * Check if message is from the current bot
     */
    isFromBot(botId) {
        return this.authorId === botId;
    }

    /**
     * Get author initials for avatar placeholder
     */
    getAuthorInitials() {
        return this.authorName
            .split(' ')
            .map(word => word.charAt(0))
            .join('')
            .substring(0, 2);
    }
}

// Export classes
window.GuildDTO = GuildDTO;
window.ChannelDTO = ChannelDTO;
window.MessageDTO = MessageDTO;