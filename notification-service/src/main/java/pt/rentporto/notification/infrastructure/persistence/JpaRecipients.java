package pt.rentporto.notification.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pt.rentporto.notification.domain.Recipient;
import pt.rentporto.notification.domain.Recipients;

/**
 * Recipients in PostgreSQL.
 */
@Repository
class JpaRecipients implements Recipients {

    private final SpringDataRecipients jpa;

    JpaRecipients(SpringDataRecipients jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Recipient> withId(UUID customerId) {
        return jpa.findById(customerId);
    }

    @Override
    public void save(Recipient recipient) {
        jpa.save(recipient);
    }
}
