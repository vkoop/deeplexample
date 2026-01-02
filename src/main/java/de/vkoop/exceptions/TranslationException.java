package de.vkoop.exceptions;

/**
 * Exception thrown when translation operations fail.
 * This includes language validation errors, API failures, and client-specific issues.
 */
public class TranslationException extends RuntimeException {

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}