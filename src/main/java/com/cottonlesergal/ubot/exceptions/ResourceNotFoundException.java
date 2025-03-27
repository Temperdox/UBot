package com.cottonlesergal.ubot.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 * This could represent a database entity, file, or any other resource that was expected to exist.
 */
public class ResourceNotFoundException extends ApiException {

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message, 404); // HTTP 404 Not Found
    }

    /**
     * Constructs a new ResourceNotFoundException for a specific resource type and identifier.
     *
     * @param resourceType the type of resource that was not found
     * @param resourceId the identifier of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId), 404);
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause, 404);
    }
}