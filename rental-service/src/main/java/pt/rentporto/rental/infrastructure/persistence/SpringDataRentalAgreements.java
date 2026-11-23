package pt.rentporto.rental.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalStatus;

interface SpringDataRentalAgreements extends JpaRepository<RentalAgreement, UUID> {

    Optional<RentalAgreement> findByReservationId(UUID reservationId);

    long countByStatus(RentalStatus status);
}
