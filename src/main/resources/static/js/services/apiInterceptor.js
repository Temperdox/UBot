/**
 * API Interceptor for authentication
 * Ensures all API requests include the auth token
 */
let apiInterceptor = {
    // Initialize the interceptor
    init() {
        const originalFetch = window.fetch;

        window.fetch = async function(url, options = {}) {
            // Only add auth header for API requests
            if (url.toString().includes('/api/')) {
                // Skip token for login and token refresh endpoints
                const isAuthEndpoint = url.toString().includes('/api/auth/login') ||
                    url.toString().includes('/api/auth/refresh-token');

                if (!isAuthEndpoint) {
                    // Get the token from storage
                    const token = localStorage.getItem('auth_token');

                    // Initialize headers if not present
                    options = options || {};
                    options.headers = options.headers || {};

                    // Add Authorization header if token exists
                    if (token) {
                        options.headers['Authorization'] = `Bearer ${token}`;
                    } else {
                        console.warn('API request made without authentication token');
                        // Redirect to login if no token available
                        window.location.href = '/login.html';
                        return Promise.reject(new Error('Authentication required'));
                    }
                }
            }

            // Call original fetch with new options
            return originalFetch(url, options);
        };

        console.log('API interceptor initialized - all API requests will include authentication token');
    },

    // Add token to provided fetch options
    addTokenToOptions(options = {}) {
        const token = localStorage.getItem('auth_token');
        if (!token) return options;

        // Initialize headers if not present
        options.headers = options.headers || {};

        // Add token
        options.headers['Authorization'] = `Bearer ${token}`;

        return options;
    }
};

// Initialize the interceptor as soon as the script loads
apiInterceptor.init();

// For direct use in code
window.apiInterceptor = apiInterceptor;