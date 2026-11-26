package pt.rentporto.notification.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pt.rentporto.notification.domain.model.Notification;

/**
 * Stored notifications, one per event. Implemented in the infrastructure.
 */
public interface NotificationLog {

    /**
     * Finds the entry of an event.
     *
     * @param eventId the event
     * @return the entry, if the event was handled before
     */
    Optional<Notification> forEvent(UUID eventId);

    /**
     * Lists the notifications of a customer.
     *
     * @param customerId the customer
     * @return the notifications, most recent attempt first
     */
    List<Notification> forCustomer(UUID customerId);

    /**
     * Stores a new entry or the changes to one.
     *
     * @param notification the entry
     */
    void save(Notification notification);
}
