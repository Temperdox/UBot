/**
 * Authentication and API Security for UBot
 * This script handles token storage, API authentication,
 * and securing API requests with JWT tokens
 */

// Token storage and management
const tokenStorage = {
    // Get token from local storage
    getToken: () => localStorage.getItem('auth_token'),

    // Set token in local storage
    setToken: (token) => {
        localStorage.setItem('auth_token', token);
        console.log('Token stored in localStorage:', token);
    },

    // Remove token from local storage
    removeToken: () => {
        console.log('Removing token from localStorage');
        localStorage.removeItem('auth_token');
    },

    // Check if a token exists
    hasToken: () => !!localStorage.getItem('auth_token')
};

// Auth functions
const auth = {
    // Login with username and password
    async login(username, password) {
        try {
            console.log('Attempting login for user:', username);
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                console.error('Login failed with status:', response.status);
                throw new Error('Login failed');
            }

            const data = await response.json();
            console.log('Login successful, received token');

            // Store token
            tokenStorage.setToken(data.token);

            return true;
        } catch (error) {
            console.error('Login error:', error);
            return false;
        }
    },

    // Logout the current user
    async logout() {
        try {
            // Call logout endpoint if token exists
            if (tokenStorage.hasToken()) {
                console.log('Sending logout request');
                await fetch('/api/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${tokenStorage.getToken()}`
                    }
                });
            }

            // Remove token regardless of API response
            tokenStorage.removeToken();

            // Redirect to login page
            window.location.href = '/login.html';
        } catch (error) {
            console.error('Logout error:', error);
            // Still remove token and redirect on error
            tokenStorage.removeToken();
            window.location.href = '/login.html';
        }
    },

    // Check if user is authenticated
    async isAuthenticated() {
        // No token means not authenticated
        if (!tokenStorage.hasToken()) {
            console.log('No token found, not authenticated');
            return false;
        }

        try {
            // Validate token with server
            console.log('Validating token with server');
            const response = await fetch('/api/auth/validate', {
                headers: {
                    'Authorization': `Bearer ${tokenStorage.getToken()}`
                }
            });

            if (!response.ok) {
                console.error('Token validation failed with status:', response.status);
                // Token is invalid, remove it
                tokenStorage.removeToken();
                return false;
            }

            const data = await response.json();
            console.log('Token validation result:', data.valid);
            return data.valid === true;
        } catch (error) {
            console.error('Authentication check error:', error);
            return false;
        }
    },

    // Get current user info
    async getUserInfo() {
        if (!tokenStorage.hasToken()) {
            console.log('No token, cannot get user info');
            return null;
        }

        try {
            console.log('Fetching user info with token');
            const response = await fetch('/api/auth/user-info', {
                headers: {
                    'Authorization': `Bearer ${tokenStorage.getToken()}`
                }
            });

            if (!response.ok) {
                console.error('User info fetch failed with status:', response.status);
                return null;
            }

            return await response.json();
        } catch (error) {
            console.error('Error getting user info:', error);
            return null;
        }
    }
};

// Intercept API calls to add authentication header
const apiInterceptor = {
    // Initialize the interceptor
    init() {
        const originalFetch = window.fetch;

        window.fetch = async function(url, options = {}) {
            // Convert relative URLs to absolute
            const requestUrl = new URL(url, window.location.origin);

            options = options || {};
            options.headers = options.headers || {};

            // Add auth header if token exists and not already set
            // Important: Always add the token to all requests except login
            if (tokenStorage.hasToken() && !options.headers['Authorization']) {
                const token = tokenStorage.getToken();
                options.headers['Authorization'] = `Bearer ${token}`;
                console.log(`Adding Authorization header to ${requestUrl.pathname}`);
            }

            // Ensure credentials are included for CORS
            options.credentials = 'include';

            return originalFetch(requestUrl.toString(), options);
        };
    }
};

// Websocket authentication helper
const wsAuth = {
    // Get authenticated websocket
    getAuthenticatedClient(url) {
        if (!tokenStorage.hasToken()) {
            console.error('No token available for WebSocket authentication');
            return null;
        }

        const token = tokenStorage.getToken();

        // Create SockJS instance with auth header
        const socket = new SockJS(url);

        // Create STOMP client with authentication
        const stompClient = Stomp.over(socket);

        // Connect with auth header
        const connectHeaders = {
            'Authorization': `Bearer ${token}`
        };

        console.log('Creating authenticated WebSocket connection');

        return {
            socket,
            stompClient,
            connectHeaders
        };
    }
};

// Initialize API interceptor
console.log('Initializing API interceptor');
apiInterceptor.init();

// Check authentication on page load
document.addEventListener('DOMContentLoaded', async () => {
    // Skip authentication check on login page
    if (window.location.pathname.includes('login.html')) {
        console.log('On login page, skipping auth check');
        return;
    }

    // Check if authenticated
    console.log('Checking authentication status');
    const isAuth = await auth.isAuthenticated();

    // Redirect to login if not authenticated
    if (!isAuth) {
        console.warn('Not authenticated, redirecting to login');
        window.location.href = '/login.html';
    } else {
        console.log('Authentication confirmed, proceeding to app');
    }
});

// Make auth functions globally available
window.auth = auth;
window.wsAuth = wsAuth;