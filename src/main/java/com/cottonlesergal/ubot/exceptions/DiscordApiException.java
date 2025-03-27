package com.cottonlesergal.ubot.exceptions;

import lombok.Getter;

/**
 * Exception thrown when there is an error interacting with the Discord API.
 * Contains Discord-specific error details.
 */
@Getter
public class DiscordApiException extends ApiException {

    private final String discordErrorCode;
    private final String discordErrorMessage;

    /**
     * Constructs a new DiscordApiException with the specified detail message.
     *
     * @param message the detail message
     */
    public DiscordApiException(String message) {
        super(message, 502); // Bad Gateway as default for third-party API failures
        this.discordErrorCode = null;
        this.discordErrorMessage = null;
    }

    /**
     * Constructs a new DiscordApiException with the specified detail message and Discord error details.
     *
     * @param message the detail message
     * @param discordErrorCode the Discord error code
     * @param discordErrorMessage the Discord error message
     */
    public DiscordApiException(String message, String discordErrorCode, String discordErrorMessage) {
        super(message, 502);
        this.discordErrorCode = discordErrorCode;
        this.discordErrorMessage = discordErrorMessage;
    }

    /**
     * Constructs a new DiscordApiException with the specified detail message, cause, and Discord error details.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     * @param discordErrorCode the Discord error code
     * @param discordErrorMessage the Discord error message
     */
    public DiscordApiException(String message, Throwable cause, String discordErrorCode, String discordErrorMessage) {
        super(message, cause, 502);
        this.discordErrorCode = discordErrorCode;
        this.discordErrorMessage = discordErrorMessage;
    }

    /**
     * Constructs a new DiscordApiException with the specified detail message, cause, status code, and Discord error details.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     * @param statusCode the HTTP status code
     * @param discordErrorCode the Discord error code
     * @param discordErrorMessage the Discord error message
     */
    public DiscordApiException(String message, Throwable cause, int statusCode,
                               String discordErrorCode, String discordErrorMessage) {
        super(message, cause, statusCode);
        this.discordErrorCode = discordErrorCode;
        this.discordErrorMessage = discordErrorMessage;
    }
}