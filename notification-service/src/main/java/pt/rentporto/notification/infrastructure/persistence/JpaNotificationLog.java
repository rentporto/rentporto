package pt.rentporto.notification.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pt.rentporto.notification.domain.model.Notification;
import pt.rentporto.notification.domain.repository.NotificationLog;

/**
 * Notification log in PostgreSQL, with the event unique.
 */
@Repository
class JpaNotificationLog implements NotificationLog {

    private final SpringDataNotifications jpa;

    JpaNotificationLog(SpringDataNotifications jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Notification> forEvent(UUID eventId) {
        return jpa.findByEventId(eventId);
    }

    @Override
    public List<Notification> forCustomer(UUID customerId) {
        return jpa.findByCustomerIdOrderByAttemptedAtDesc(customerId);
    }

    @Override
    public void save(Notification notification) {
        jpa.save(notification);
    }
}
