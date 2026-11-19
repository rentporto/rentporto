package pt.rentporto.rental.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;

/**
 * Rental agreements in PostgreSQL. The reservation is unique, so a second agreement of the same reservation fails on
 * the flush.
 */
@Repository
class JpaRentalAgreements implements RentalAgreements {

    private final SpringDataRentalAgreements jpa;
    private final EntityManager entityManager;

    JpaRentalAgreements(SpringDataRentalAgreements jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<RentalAgreement> withId(UUID rentalId) {
        return jpa.findById(rentalId);
    }

    @Override
    public Optional<RentalAgreement> lockWithId(UUID rentalId) {
        return Optional.ofNullable(entityManager.find(RentalAgreement.class, rentalId, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    public Optional<RentalAgreement> forReservation(UUID reservationId) {
        return jpa.findByReservationId(reservationId);
    }

    @Override
    public void add(RentalAgreement agreement) {
        entityManager.persist(agreement);
        entityManager.flush();
    }

    @Override
    public void update(RentalAgreement agreement) {
        entityManager.flush();
    }
}
