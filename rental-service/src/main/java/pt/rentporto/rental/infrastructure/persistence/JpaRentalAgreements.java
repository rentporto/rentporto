package pt.rentporto.rental.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalStatus;

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
    public List<RentalAgreement> matching(String station, RentalStatus status) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RentalAgreement> query = builder.createQuery(RentalAgreement.class);
        Root<RentalAgreement> agreement = query.from(RentalAgreement.class);
        List<Predicate> filters = new ArrayList<>();
        if (station != null) {
            filters.add(builder.or(
                    builder.equal(agreement.get("pickupStation"), station),
                    builder.equal(agreement.get("returnStation"), station)));
        }
        if (status != null) {
            filters.add(builder.equal(agreement.get("status"), status));
        }
        query.where(filters.toArray(Predicate[]::new))
                .orderBy(builder.asc(agreement.get("period").get("pickupAt")));
        return entityManager.createQuery(query).getResultList();
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
