package pt.rentporto.notification.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pt.rentporto.notification.domain.PickupReminder;
import pt.rentporto.notification.domain.PickupReminderStatus;
import pt.rentporto.notification.domain.PickupReminders;

/**
 * Pick-up reminders in memory.
 */
final class InMemoryPickupReminders implements PickupReminders {

    private final Map<UUID, PickupReminder> byReservation = new HashMap<>();

    @Override
    public Optional<PickupReminder> forReservation(UUID reservationId) {
        return Optional.ofNullable(byReservation.get(reservationId));
    }

    @Override
    public List<PickupReminder> pendingWithPickupBetween(Instant from, Instant until) {
        return byReservation.values().stream()
                .filter(reminder -> reminder.status() == PickupReminderStatus.PENDING)
                .filter(reminder -> !reminder.pickupAt().isBefore(from) && !reminder.pickupAt().isAfter(until))
                .sorted(Comparator.comparing(PickupReminder::pickupAt))
                .toList();
    }

    @Override
    public void save(PickupReminder reminder) {
        byReservation.put(reminder.reservationId(), reminder);
    }
}
