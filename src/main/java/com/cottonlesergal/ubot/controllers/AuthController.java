package com.cottonlesergal.ubot.controllers;

import com.cottonlesergal.ubot.exceptions.AuthenticationException;
import com.cottonlesergal.ubot.services.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for authentication-related endpoints.
 * Provides API for user authentication.
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    // Make sure you have the right service injected
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        log.debug("REST request to login for user: {}", username);

        try {
            String token = authService.authenticateUser(username, password);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", username);

            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            log.error("Error during authentication", e);

            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> refreshRequest) {
        String refreshToken = refreshRequest.get("refreshToken");

        log.debug("REST request to refresh token");

        String newToken = authService.refreshToken(refreshToken);

        Map<String, String> response = new HashMap<>();
        response.put("token", newToken);

        return ResponseEntity.ok(response);
    }

    /**
     * Validate token endpoint
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix

        log.debug("REST request to validate token");

        boolean isValid = authService.validateToken(token);

        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);

        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix

        log.debug("REST request to logout");

        authService.invalidateToken(token);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get user information from token
     */
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix

        log.debug("REST request to get user info from token");

        Map<String, Object> userInfo = authService.getUserInfoFromToken(token);

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Change password endpoint
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> passwordChangeRequest) {

        String token = authHeader.substring(7); // Remove "Bearer " prefix
        String currentPassword = passwordChangeRequest.get("currentPassword");
        String newPassword = passwordChangeRequest.get("newPassword");

        log.debug("REST request to change password");

        authService.changePassword(token, currentPassword, newPassword);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Handle authentication errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(Exception ex) {
        log.error("Authentication error: {}", ex.getMessage());

        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}