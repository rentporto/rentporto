package pt.rentporto.fleet.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import pt.rentporto.fleet.domain.StatusChange;
import pt.rentporto.fleet.domain.StatusChanges;

/**
 * History of the states of the vehicles in PostgreSQL.
 */
@Repository
class JpaStatusChanges implements StatusChanges {

    private final EntityManager entityManager;

    JpaStatusChanges(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void add(StatusChange change) {
        entityManager.persist(change);
    }
}
