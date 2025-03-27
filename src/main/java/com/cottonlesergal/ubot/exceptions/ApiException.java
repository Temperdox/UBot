package com.cottonlesergal.ubot.exceptions;

import lombok.Getter;

/**
 * Base exception class for API related exceptions in the UBot application.
 * Serves as a parent class for more specific API exceptions.
 */
@Getter
public class ApiException extends RuntimeException {

    private final int statusCode;

    /**
     * Constructs a new ApiException with the specified detail message.
     *
     * @param message the detail message
     */
    public ApiException(String message) {
        this(message, 500);
    }

    /**
     * Constructs a new ApiException with the specified detail message and status code.
     *
     * @param message the detail message
     * @param statusCode the HTTP status code
     */
    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs a new ApiException with the specified detail message, cause, and status code.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     * @param statusCode the HTTP status code
     */
    public ApiException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}