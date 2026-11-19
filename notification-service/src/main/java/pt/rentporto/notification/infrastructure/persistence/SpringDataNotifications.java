package pt.rentporto.notification.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.notification.domain.Notification;

interface SpringDataNotifications extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByEventId(UUID eventId);

    List<Notification> findByCustomerIdOrderByAttemptedAtDesc(UUID customerId);
}
