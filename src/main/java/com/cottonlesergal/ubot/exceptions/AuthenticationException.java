package com.cottonlesergal.ubot.exceptions;

/**
 * Exception thrown when authentication or authorization fails
 */
public class AuthenticationException extends RuntimeException {

    /**
     * Constructs a new authentication exception with null as its detail message.
     */
    public AuthenticationException() {
        super();
    }

    /**
     * Constructs a new authentication exception with the specified detail message.
     *
     * @param message the detail message
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Constructs a new authentication exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new authentication exception with the specified cause.
     *
     * @param cause the cause
     */
    public AuthenticationException(Throwable cause) {
        super(cause);
    }
}