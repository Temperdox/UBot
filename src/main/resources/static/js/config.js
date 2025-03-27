/**
 * Configuration for Discord Bot Control Panel
 */
let CONFIG = {
    // Application info
    APP_NAME: 'Discord Bot Control Panel',
    APP_VERSION: '1.0.0',

    // API configuration
    API: {
        BASE_URL: '/api',
        TIMEOUT: 30000, // 30 seconds
        RETRY_ATTEMPTS: 3,
        DEFAULT_ERROR_MESSAGE: 'An error occurred while communicating with the server.',
        CACHE_DURATION: 5 * 60 * 1000, // 5 minutes
    },

    // WebSocket configuration
    WEBSOCKET: {
        ENDPOINT: '/ws',
        RECONNECT_INTERVAL: 2000, // 2 seconds
        MAX_RECONNECT_ATTEMPTS: 10,
        HEARTBEAT_INTERVAL: 30000, // 30 seconds
        HEARTBEAT_TIMEOUT: 10000, // 10 seconds
    },

    // UI configuration
    UI: {
        MESSAGE_LIMIT: 50, // Number of messages to load at once
        MAX_MESSAGES: 200, // Maximum number of messages to keep in memory
        TYPING_INDICATOR_TIMEOUT: 10000, // 10 seconds
        TOAST_DURATION: 5000, // 5 seconds
        ANIMATION_DURATION: 300, // 300 milliseconds
    },

    // Discord limits
    DISCORD: {
        MAX_MESSAGE_LENGTH: 2000,
        MAX_EMBED_LENGTH: 4000,
        MAX_FIELD_LENGTH: 1024,
        MAX_TITLE_LENGTH: 256,
        MAX_DESCRIPTION_LENGTH: 2048,
        MAX_FIELD_NAME_LENGTH: 256,
        MAX_FIELD_VALUE_LENGTH: 1024,
        MAX_FIELDS: 25,
    },

    // Debug mode (auto-enabled on localhost)
    DEBUG: window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1',

    // Date formatting
    DATE_FORMAT: {
        TIME: 'HH:mm',
        DATE: 'MM/DD/YYYY',
        DATETIME: 'MM/DD/YYYY HH:mm',
        RELATIVE: true, // Use relative time when possible (e.g., "2 minutes ago")
    },
};

// Export configuration
export default CONFIG;

// For backwards compatibility with non-module code
window.CONFIG = CONFIG;