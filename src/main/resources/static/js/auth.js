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
    },

    // Remove token from local storage
    removeToken: () => {
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
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                throw new Error('Login failed');
            }

            const data = await response.json();

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
            return false;
        }

        try {
            // Validate token with server
            const response = await fetch('/api/auth/validate', {
                headers: {
                    'Authorization': `Bearer ${tokenStorage.getToken()}`
                }
            });

            if (!response.ok) {
                // Token is invalid, remove it
                tokenStorage.removeToken();
                return false;
            }

            const data = await response.json();
            return data.valid === true;
        } catch (error) {
            console.error('Authentication check error:', error);
            return false;
        }
    },

    // Get current user info
    async getUserInfo() {
        if (!tokenStorage.hasToken()) {
            return null;
        }

        try {
            const response = await fetch('/api/auth/user-info', {
                headers: {
                    'Authorization': `Bearer ${tokenStorage.getToken()}`
                }
            });

            if (!response.ok) {
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

            // Only intercept API calls
            if (requestUrl.pathname.startsWith('/api/') &&
                !requestUrl.pathname.includes('/api/auth/login') &&
                !requestUrl.pathname.includes('/api/auth/refresh-token')) {

                options = options || {};
                options.headers = options.headers || {};

                // Add auth header if token exists
                if (tokenStorage.hasToken()) {
                    options.headers['Authorization'] = `Bearer ${tokenStorage.getToken()}`;
                }

                // Ensure credentials are included for CORS
                options.credentials = 'include';
            }

            return originalFetch(requestUrl.toString(), options);
        };
    }
};

// Initialize API interceptor
apiInterceptor.init();

// Check authentication on page load
document.addEventListener('DOMContentLoaded', async () => {
    // Skip authentication check on login page
    if (window.location.pathname.includes('login.html')) {
        return;
    }

    // Check if authenticated
    const isAuth = await auth.isAuthenticated();

    // Redirect to log in if not authenticated
    if (!isAuth) {
        window.location.href = '/login.html';
    }
});

// Make auth functions globally available
window.auth = auth;