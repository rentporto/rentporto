package pt.rentporto.notification.application.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pt.rentporto.notification.domain.model.Notification;
import pt.rentporto.notification.domain.repository.NotificationLog;

/**
 * Notification log in memory.
 */
final class InMemoryLog implements NotificationLog {

    private final Map<UUID, Notification> byEvent = new HashMap<>();

    @Override
    public Optional<Notification> forEvent(UUID eventId) {
        return Optional.ofNullable(byEvent.get(eventId));
    }

    @Override
    public List<Notification> forCustomer(UUID customerId) {
        return byEvent.values().stream()
                .filter(notification -> notification.customerId().equals(customerId))
                .sorted(Comparator.comparing(Notification::attemptedAt).reversed())
                .toList();
    }

    @Override
    public void save(Notification notification) {
        byEvent.put(notification.eventId(), notification);
    }
}
