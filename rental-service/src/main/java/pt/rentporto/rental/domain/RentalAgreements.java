package pt.rentporto.rental.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Stored rental agreements. Implemented in the infrastructure.
 */
public interface RentalAgreements {

    /**
     * Finds an agreement.
     *
     * @param rentalId identifier of the agreement
     * @return the agreement, if there is one
     */
    Optional<RentalAgreement> withId(UUID rentalId);

    /**
     * Finds an agreement and locks it until the end of the transaction, so that two changes to it happen one after
     * the other.
     *
     * @param rentalId identifier of the agreement
     * @return the agreement, if there is one
     */
    Optional<RentalAgreement> lockWithId(UUID rentalId);

    /**
     * Finds the agreement of a reservation.
     *
     * @param reservationId the reservation
     * @return the agreement, if there is one
     */
    Optional<RentalAgreement> forReservation(UUID reservationId);

    /**
     * Stores a new agreement. A reservation has one agreement: storing a second one fails.
     *
     * @param agreement the agreement
     */
    void add(RentalAgreement agreement);

    /**
     * Stores the changes to an agreement.
     *
     * @param agreement the agreement
     */
    void update(RentalAgreement agreement);
}
