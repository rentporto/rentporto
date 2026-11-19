package pt.rentporto.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pt.rentporto.notification.domain.PickupReminder;
import pt.rentporto.notification.domain.PickupReminderStatus;
import pt.rentporto.notification.domain.PickupReminders;

/**
 * Pick-up reminders in PostgreSQL, keyed by the reservation.
 */
@Repository
class JpaPickupReminders implements PickupReminders {

    private final SpringDataPickupReminders jpa;

    JpaPickupReminders(SpringDataPickupReminders jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PickupReminder> forReservation(UUID reservationId) {
        return jpa.findById(reservationId);
    }

    @Override
    public List<PickupReminder> pendingWithPickupBetween(Instant from, Instant until) {
        return jpa.findByStatusAndPickupAtBetweenOrderByPickupAt(PickupReminderStatus.PENDING, from, until);
    }

    @Override
    public void save(PickupReminder reminder) {
        jpa.save(reminder);
    }
}
