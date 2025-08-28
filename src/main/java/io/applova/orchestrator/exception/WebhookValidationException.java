package io.applova.orchestrator.exception;

/**
 * Exception thrown when webhook validation fails.
 * This can include scenarios like invalid secret, unauthorized access, etc.
 */
public class WebhookValidationException extends RuntimeException {
    /**
     * Constructs a new WebhookValidationException with the specified detail message.
     *
     * @param message the detail message
     */
    public WebhookValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new WebhookValidationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public WebhookValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
