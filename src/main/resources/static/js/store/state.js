/**
 * Application state for Discord Bot Control Panel
 * Central state store following a Vuex/Redux-like pattern
 */

// Define initial state
const initialState = {
    // Current user (bot)
    currentUser: null,

    // Application status
    status: {
        connected: false,
        loading: false,
        error: null
    },

    // Discord entities
    guilds: [],
    channels: [],
    users: [],
    messages: [],
    dmUsers: [],

    // Active selections
    selectedGuildId: null,
    selectedChannelId: null,
    selectedDmUserId: null,
    isDmView: true,

    // UI state
    typingUsers: new Map(), // userId -> timestamp
    notifications: []
};

// Create a copy of the state to avoid direct modification
const state = JSON.parse(JSON.stringify(initialState));

// Add non-serializable properties back
state.typingUsers = new Map();

/**
 * Get the current state
 * @param {string} key - Optional key to get specific part of state
 * @returns {*} - Current state or part of state
 */
function getState(key = null) {
    if (key === null) {
        return state;
    }

    // Handle nested keys with dot notation (e.g., 'status.connected')
    if (key.includes('.')) {
        const parts = key.split('.');
        let current = state;

        for (const part of parts) {
            if (current === undefined || current === null) {
                return undefined;
            }
            current = current[part];
        }

        return current;
    }

    return state[key];
}

/**
 * Reset state to initial values
 */
function resetState() {
    // Clear all properties
    Object.keys(state).forEach(key => {
        delete state[key];
    });

    // Copy initial state
    const initial = JSON.parse(JSON.stringify(initialState));
    Object.keys(initial).forEach(key => {
        state[key] = initial[key];
    });

    // Restore non-serializable properties
    state.typingUsers = new Map();
}

// Export state and functions
export {
    state,
    getState,
    resetState
};

// For backwards compatibility with non-module code
window.appState = state;