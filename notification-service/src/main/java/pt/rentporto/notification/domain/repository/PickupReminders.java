package pt.rentporto.notification.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pt.rentporto.notification.domain.model.PickupReminder;

/**
 * Stored pick-up reminders, one per reservation. Implemented in the infrastructure.
 */
public interface PickupReminders {

    /**
     * Finds the reminder of a reservation.
     *
     * @param reservationId the reservation
     * @return the reminder, if the reservation was confirmed, cancelled or picked up before
     */
    Optional<PickupReminder> forReservation(UUID reservationId);

    /**
     * Lists the reminders still to be sent whose pick-up falls between two instants, both included.
     *
     * @param from  earliest pick-up
     * @param until latest pick-up
     * @return the reminders, soonest pick-up first
     */
    List<PickupReminder> pendingWithPickupBetween(Instant from, Instant until);

    /**
     * Stores a new reminder or the changes to one.
     *
     * @param reminder the reminder
     */
    void save(PickupReminder reminder);
}
