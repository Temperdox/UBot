package com.cottonlesergal.ubot.services;

import com.cottonlesergal.ubot.entities.AuthToken;
import com.cottonlesergal.ubot.entities.UserCredential;
import com.cottonlesergal.ubot.exceptions.AuthenticationException;
import com.cottonlesergal.ubot.providers.JDAProvider;
import com.cottonlesergal.ubot.repositories.AuthTokenRepository;
import com.cottonlesergal.ubot.repositories.UserCredentialRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Discord authentication and permission management
 */
@Service
@Slf4j
public class AuthService {

    private final JDAProvider jda;
    private final UserCredentialRepository userCredentialRepository;
    private final AuthTokenRepository authTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.secret:defaultSecretKeyThatShouldBeReplacedInProduction}")
    private String secret;

    @Value("${jwt.expiration:3600}") // Default to 1 hour
    private long jwtExpirationInSeconds;

    @Value("${jwt.refresh.expiration:86400}") // Default to 24 hours
    private long refreshExpirationInSeconds;

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Autowired
    public AuthService(JDAProvider jda,
                       UserCredentialRepository userCredentialRepository,
                       AuthTokenRepository authTokenRepository) {
        this.jda = jda;
        this.userCredentialRepository = userCredentialRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Authenticate a user and generate JWT tokens
     *
     * @param username The username
     * @param password The password
     * @return JWT access token
     * @throws AuthenticationException if authentication fails
     */
    public String authenticateUser(String username, String password) {
        Optional<UserCredential> userOpt = userCredentialRepository.findByUsername(username);

        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            log.warn("Failed login attempt for username: {}", username);
            throw new AuthenticationException("Invalid username or password");
        }

        UserCredential user = userOpt.get();

        // Generate tokens
        String token = generateToken(user);
        String refreshToken = generateRefreshToken(user);

        // Store tokens in database - FIRST CHECK IF TOKEN EXISTS
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(jwtExpirationInSeconds);
        LocalDateTime refreshExpiresAt = now.plusSeconds(refreshExpirationInSeconds);

        // Check if user already has a token
        Optional<AuthToken> existingTokenOpt = authTokenRepository.findByUserId(user.getId());

        AuthToken authToken;
        if (existingTokenOpt.isPresent()) {
            // Update existing token
            authToken = existingTokenOpt.get();
            authToken.setToken(token);
            authToken.setRefreshToken(refreshToken);
            authToken.setExpiresAt(expiresAt);
            authToken.setRefreshExpiresAt(refreshExpiresAt);
            authToken.setActive(true);
        } else {
            // Create new token
            authToken = AuthToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .refreshExpiresAt(refreshExpiresAt)
                    .active(true)
                    .build();
        }

        // Save token
        try {
            authTokenRepository.save(authToken);
        } catch (Exception e) {
            log.error("Error saving token: {}", e.getMessage());
            // Still return token even if saving fails
        }

        log.info("User authenticated: {}", username);
        return token;
    }

    @Scheduled(fixedRate = 86400000) // Run once a day
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        authTokenRepository.deleteByExpiresAtBefore(now);
        log.info("Expired tokens cleanup completed");
    }

    /**
     * Refresh an access token using a refresh token
     *
     * @param refreshToken The refresh token
     * @return New JWT access token
     * @throws AuthenticationException if refresh token is invalid or expired
     */
    public String refreshToken(String refreshToken) {
        Optional<AuthToken> tokenOpt = authTokenRepository.findByRefreshToken(refreshToken);

        if (tokenOpt.isEmpty()) {
            log.warn("Refresh token not found: {}", refreshToken);
            throw new AuthenticationException("Invalid refresh token");
        }

        AuthToken token = tokenOpt.get();

        // Check if refresh token is expired
        if (token.getRefreshExpiresAt().isBefore(LocalDateTime.now()) || !token.isActive()) {
            log.warn("Refresh token expired or inactive: {}", refreshToken);
            throw new AuthenticationException("Refresh token expired");
        }

        // Get user
        Optional<UserCredential> userOpt = userCredentialRepository.findById(token.getUserId());

        if (userOpt.isEmpty()) {
            log.error("User not found for token: {}", token.getUserId());
            throw new AuthenticationException("User not found");
        }

        UserCredential user = userOpt.get();

        // Generate new access token
        String newToken = generateToken(user);

        // Update token in database
        token.setToken(newToken);
        token.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpirationInSeconds));
        authTokenRepository.save(token);

        log.info("Token refreshed for user: {}", user.getUsername());
        return newToken;
    }

    /**
     * Validate a JWT token
     *
     * @param token The JWT token
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setAllowedClockSkewSeconds(600)
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.info("JWT signature valid. Subject: {}", claims.getSubject());

            Optional<AuthToken> tokenOpt = authTokenRepository.findByToken(token);

            if (tokenOpt.isPresent()) {
                log.info("Token found in DB. Active: {}", tokenOpt.get().isActive());
            } else {
                log.warn("Token NOT found in DB.");
            }

            return tokenOpt.isPresent() && tokenOpt.get().isActive();
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Invalidate a JWT token
     *
     * @param token The JWT token
     */
    public void invalidateToken(String token) {
        Optional<AuthToken> tokenOpt = authTokenRepository.findByToken(token);

        if (tokenOpt.isPresent()) {
            AuthToken authToken = tokenOpt.get();
            authToken.setActive(false);
            authTokenRepository.save(authToken);
            log.info("Token invalidated: {}", token);
        } else {
            log.warn("Token not found for invalidation: {}", token);
        }
    }

    /**
     * Get user information from a JWT token
     *
     * @param token The JWT token
     * @return Map of user information
     * @throws AuthenticationException if token is invalid
     */
    public Map<String, Object> getUserInfoFromToken(String token) {
        if (!validateToken(token)) {
            throw new AuthenticationException("Invalid token");
        }

        // Parse token claims
        Claims claims = Jwts.parserBuilder()
                .setAllowedClockSkewSeconds(600)
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Get user ID from token
        String userId = claims.getSubject();

        // Get user from database
        Optional<UserCredential> userOpt = userCredentialRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new AuthenticationException("User not found");
        }

        UserCredential user = userOpt.get();

        // Create user info map
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("discordId", user.getDiscordId());

        if (user.getDiscordId() != null) {
            // Add Discord user info if available
            try {
                net.dv8tion.jda.api.entities.User discordUser =
                        jda.getJda().getUserById(user.getDiscordId());

                if (discordUser != null) {
                    userInfo.put("discordName", discordUser.getName());
                    userInfo.put("discordAvatar", discordUser.getEffectiveAvatarUrl());
                }
            } catch (Exception e) {
                log.warn("Failed to get Discord user info", e);
                // Continue without Discord info
            }
        }

        return userInfo;
    }

    /**
     * Change a user's password
     *
     * @param token The JWT token for authentication
     * @param currentPassword The user's current password
     * @param newPassword The new password
     * @throws AuthenticationException if token or current password is invalid
     */
    public void changePassword(String token, String currentPassword, String newPassword) {
        if (!validateToken(token)) {
            throw new AuthenticationException("Invalid token");
        }

        // Parse token claims
        Claims claims = Jwts.parserBuilder()
                .setAllowedClockSkewSeconds(600)
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Get user ID from token
        String userId = claims.getSubject();

        // Get user from database
        Optional<UserCredential> userOpt = userCredentialRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new AuthenticationException("User not found");
        }

        UserCredential user = userOpt.get();

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userCredentialRepository.save(user);

        // Invalidate all existing tokens for this user
        Optional<AuthToken> tokenOpt = authTokenRepository.findByUserId(userId);
        if (tokenOpt.isPresent()) {
            tokenOpt.get().setActive(false);
            authTokenRepository.save(tokenOpt.get());
        }

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Generate a JWT token for a user
     *
     * @param user The user
     * @return JWT token
     */
    private String generateToken(UserCredential user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInSeconds * 1000);

        return Jwts.builder()
                .setSubject(user.getId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("username", user.getUsername())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate a refresh token for a user
     *
     * @param user The user
     * @return Refresh token
     */
    private String generateRefreshToken(UserCredential user) {
        // Simple UUID-based refresh token
        return UUID.randomUUID().toString();
    }

    /**
     * Register a new user account
     *
     * @param username The username
     * @param password The password
     * @param discordId The Discord user ID (optional)
     * @return The created user
     * @throws AuthenticationException if username is already taken
     */
    public UserCredential registerUser(String username, String password, String discordId) {
        // Check if username already exists
        if (userCredentialRepository.findByUsername(username).isPresent()) {
            throw new AuthenticationException("Username already taken");
        }

        // Check if Discord ID is already linked to another account
        if (discordId != null && userCredentialRepository.findByDiscordId(discordId).isPresent()) {
            throw new AuthenticationException("Discord account already linked to another user");
        }

        // Create new user
        UserCredential user = UserCredential.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .discordId(discordId)
                .build();

        user = userCredentialRepository.save(user);
        log.info("New user registered: {}", username);

        return user;
    }

    /**
     * Link a Discord account to an existing user account
     *
     * @param username The username
     * @param discordId The Discord user ID
     * @throws AuthenticationException if user not found or Discord ID already linked
     */
    public void linkDiscordAccount(String username, String discordId) {
        // Get user
        Optional<UserCredential> userOpt = userCredentialRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new AuthenticationException("User not found");
        }

        // Check if Discord ID is already linked to another account
        if (userCredentialRepository.findByDiscordId(discordId).isPresent()) {
            throw new AuthenticationException("Discord account already linked to another user");
        }

        // Update user
        UserCredential user = userOpt.get();
        user.setDiscordId(discordId);
        userCredentialRepository.save(user);

        log.info("Discord account linked to user: {}", username);
    }

    // ... existing permission check methods ...
}