/**
 * User DTO for Discord users
 */
class UserDTO {
    constructor(data = {}) {
        this.id = data.id || '';
        this.name = data.name || 'Unknown User';
        this.discriminator = data.discriminator || '0000';
        this.avatarUrl = data.avatarUrl || '';
        this.bot = data.bot || false;
        this.nickname = data.nickname || null;
        this.status = data.status || 'offline';
        this.roles = data.roles || [];
    }

    /**
     * Get user's display name (nickname or username)
     */
    getDisplayName() {
        return this.nickname || this.name;
    }

    /**
     * Get user's initials for avatar placeholder
     */
    getInitials() {
        return this.getDisplayName()
            .split(' ')
            .map(word => word.charAt(0))
            .join('')
            .substring(0, 2);
    }

    /**
     * Check if user is online
     */
    isOnline() {
        return this.status !== 'offline' && this.status !== 'invisible';
    }

    /**
     * Get a CSS class for user status
     */
    getStatusClass() {
        switch (this.status.toLowerCase()) {
            case 'online':
                return 'online';
            case 'idle':
                return 'idle';
            case 'dnd':
            case 'do_not_disturb':
                return 'dnd';
            case 'streaming':
                return 'streaming';
            case 'offline':
            case 'invisible':
            default:
                return 'offline';
        }
    }

    /**
     * Check if user has a role
     */
    hasRole(roleName) {
        return this.roles.some(role =>
            role.toLowerCase() === roleName.toLowerCase()
        );
    }

    /**
     * Check if user is a server owner
     */
    isOwner(guildOwnerId) {
        return this.id === guildOwnerId;
    }

    /**
     * Get full username with discriminator
     */
    getFullUsername() {
        return `${this.name}#${this.discriminator}`;
    }
}

// Export class
window.UserDTO = UserDTO;