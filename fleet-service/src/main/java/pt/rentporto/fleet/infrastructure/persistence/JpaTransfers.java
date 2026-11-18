package pt.rentporto.fleet.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.Transfer;
import pt.rentporto.fleet.domain.Transfers;

/**
 * Transfers in PostgreSQL. A partial unique index keeps at most one open transfer per vehicle, as a safety net under
 * the lock of the vehicle.
 */
@Repository
class JpaTransfers implements Transfers {

    private final EntityManager entityManager;

    JpaTransfers(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Transfer> withId(UUID id) {
        return Optional.ofNullable(entityManager.find(Transfer.class, id));
    }

    @Override
    public Optional<Transfer> startedWith(LicensePlate plate, String key) {
        return entityManager.createQuery(
                        "SELECT t FROM Transfer t WHERE t.plate = :plate AND t.idempotencyKey = :key", Transfer.class)
                .setParameter("plate", plate)
                .setParameter("key", key)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public void add(Transfer transfer) {
        entityManager.persist(transfer);
        entityManager.flush();
    }

    @Override
    public void update(Transfer transfer) {
        entityManager.flush();
    }
}
