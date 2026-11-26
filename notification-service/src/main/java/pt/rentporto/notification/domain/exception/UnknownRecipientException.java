package pt.rentporto.notification.domain.exception;

import java.util.UUID;

/**
 * An event is about a customer the service does not know yet, so there is no address to send it to. The event is
 * tried again, and goes to the dead letter queue if the customer is still unknown.
 */
public class UnknownRecipientException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param customerId the customer
     */
    public UnknownRecipientException(UUID customerId) {
        super("There is no email for the customer " + customerId);
    }
}
