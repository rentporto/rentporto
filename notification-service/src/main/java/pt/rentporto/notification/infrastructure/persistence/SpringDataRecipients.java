package pt.rentporto.notification.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.notification.domain.model.Recipient;

interface SpringDataRecipients extends JpaRepository<Recipient, UUID> {
}
