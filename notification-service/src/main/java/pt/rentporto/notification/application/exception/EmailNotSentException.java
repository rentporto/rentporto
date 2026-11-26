package pt.rentporto.notification.application.exception;

/**
 * The mail server did not accept an email. The event is tried again, and goes to the dead letter queue after its
 * attempts.
 */
public class EmailNotSentException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what happened
     * @param cause   the failure of the mail server
     */
    public EmailNotSentException(String message, Throwable cause) {
        super(message, cause);
    }
}
