package pt.rentporto.fleet.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import pt.rentporto.fleet.domain.RentalAlreadyAssignedException;
import pt.rentporto.fleet.domain.VehicleAssignment;
import pt.rentporto.fleet.domain.VehicleAssignments;

/**
 * Assignments in PostgreSQL. The lock of a rental is a transaction-scoped advisory lock on its id; the rental is also
 * the primary key, as a safety net under that lock.
 */
@Repository
class JpaVehicleAssignments implements VehicleAssignments {

    static final String PRIMARY_KEY = "vehicle_assignments_pkey";

    private final EntityManager entityManager;

    JpaVehicleAssignments(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lock(UUID rentalId) {
        entityManager.createNativeQuery("SELECT CAST(pg_advisory_xact_lock(:key) AS TEXT)")
                .setParameter("key", rentalId.getMostSignificantBits() ^ rentalId.getLeastSignificantBits())
                .getSingleResult();
    }

    @Override
    public Optional<VehicleAssignment> forRental(UUID rentalId) {
        return Optional.ofNullable(entityManager.find(VehicleAssignment.class, rentalId));
    }

    @Override
    public void add(VehicleAssignment assignment) {
        try {
            entityManager.persist(assignment);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (PRIMARY_KEY.equals(constraintName(e))) {
                throw new RentalAlreadyAssignedException(assignment.rentalId());
            }
            throw e;
        }
    }

    @Override
    public void update(VehicleAssignment assignment) {
        entityManager.flush();
    }

    private static String constraintName(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
