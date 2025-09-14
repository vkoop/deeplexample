package de.vkoop.exceptions;

/**
 * Exception thrown when configuration loading or validation fails.
 * This includes missing auth keys, invalid config files, and missing required configuration.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}