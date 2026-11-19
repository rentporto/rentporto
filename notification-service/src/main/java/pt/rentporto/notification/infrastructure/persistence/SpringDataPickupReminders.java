package pt.rentporto.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.notification.domain.PickupReminder;
import pt.rentporto.notification.domain.PickupReminderStatus;

interface SpringDataPickupReminders extends JpaRepository<PickupReminder, UUID> {

    List<PickupReminder> findByStatusAndPickupAtBetweenOrderByPickupAt(
            PickupReminderStatus status, Instant from, Instant until);
}
