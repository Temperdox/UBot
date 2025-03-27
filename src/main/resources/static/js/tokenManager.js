/**
 * Token manager for automatically refreshing authentication
 */
let tokenManager = {
    // Keep track of the refresh timer
    refreshTimer: null,

    // Initialize token management
    init() {
        console.log('Initializing token manager');
        // Clear any existing timer
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
        }

        // Schedule first refresh check
        this.scheduleRefresh();
    },

    // Schedule token refresh
    scheduleRefresh() {
        const token = localStorage.getItem('auth_token');
        if (!token) return;

        try {
            // Get expiration time from token payload
            const payload = JSON.parse(atob(token.split('.')[1]));
            const expiresAt = payload.exp * 1000; // Convert to milliseconds
            const currentTime = Date.now();

            // Calculate time until expiration (in milliseconds)
            const timeUntilExpiry = expiresAt - currentTime;

            // Refresh when 90% of the way through the token's lifetime
            const refreshTime = Math.max(timeUntilExpiry * 0.1, 60000); // At least 1 minute before expiry

            console.log(`Token expires in ${Math.round(timeUntilExpiry/1000)} seconds. Will refresh in ${Math.round(refreshTime/1000)} seconds`);

            // Schedule refresh
            this.refreshTimer = setTimeout(() => this.refreshToken(), refreshTime);
        } catch (error) {
            console.error('Error scheduling token refresh:', error);
        }
    },

    // Perform actual token refresh
    async refreshToken() {
        console.log('Attempting to refresh token');
        try {
            const currentToken = localStorage.getItem('auth_token');
            if (!currentToken) return;

            // Call refresh endpoint
            const response = await fetch('/api/auth/refresh-token', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${currentToken}`
                },
                body: JSON.stringify({
                    refreshToken: currentToken // Your backend expects the token in this format
                })
            });

            if (response.ok) {
                const data = await response.json();

                // Store new token
                localStorage.setItem('auth_token', data.token);
                console.log('Token refreshed successfully');

                // Schedule next refresh
                this.scheduleRefresh();
            } else {
                console.error('Failed to refresh token, status:', response.status);
                // Redirect to login if refresh failed
                window.location.href = '/login.html?error=session_expired';
            }
        } catch (error) {
            console.error('Error refreshing token:', error);
            // Redirect to login on error
            window.location.href = '/login.html?error=session_expired';
        }
    }
};

// Initialize token manager
document.addEventListener('DOMContentLoaded', () => {
    tokenManager.init();
});

// Re-init when token changes
window.addEventListener('storage', (event) => {
    if (event.key === 'auth_token') {
        tokenManager.init();
    }
});

// Add to window for global access
window.tokenManager = tokenManager;