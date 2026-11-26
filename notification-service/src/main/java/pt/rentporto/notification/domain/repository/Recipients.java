package pt.rentporto.notification.domain.repository;

import java.util.Optional;
import java.util.UUID;
import pt.rentporto.notification.domain.model.Recipient;

/**
 * Stored recipients. Implemented in the infrastructure.
 */
public interface Recipients {

    /**
     * Finds a recipient.
     *
     * @param customerId id of the customer
     * @return the recipient, if the service knows the customer
     */
    Optional<Recipient> withId(UUID customerId);

    /**
     * Stores a new recipient or the changes to one.
     *
     * @param recipient the recipient
     */
    void save(Recipient recipient);
}
