package pt.rentporto.rental.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.rental.domain.ConfirmedReservation;
import pt.rentporto.rental.domain.DiscardReason;
import pt.rentporto.rental.domain.InconsistentEventException;
import pt.rentporto.rental.domain.Money;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;

/**
 * Keeps the rental agreements in step with the reservations: a confirmed reservation gets an agreement waiting for
 * the pick-up, and a cancelled or not picked up one discards it, releasing the deposit a failed pick-up left held. A
 * reservation has one agreement.
 */
@Service
public class PendingAgreements {

    private final RentalAgreements agreements;
    private final RentalEvents events;
    private final TransactionTemplate transaction;

    PendingAgreements(
            RentalAgreements agreements, RentalEvents events, PlatformTransactionManager transactionManager) {
        this.agreements = agreements;
        this.events = events;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Creates the agreement of a confirmed reservation, unless the reservation already has one.
     *
     * @param reservation the reservation
     * @return {@code true} if the agreement was created now
     */
    public boolean reservationConfirmed(ConfirmedReservation reservation) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            if (agreements.forReservation(reservation.reservationId()).isPresent()) {
                return false;
            }
            agreements.add(RentalAgreement.awaitingPickup(reservation));
            return true;
        }));
    }

    /**
     * Discards the agreement of a reservation that was cancelled or not picked up.
     *
     * @param reservationId the reservation
     * @param reason        why
     * @return {@code true} if it was discarded now, {@code false} if it already was
     * @throws InconsistentEventException if the reservation has no agreement or its vehicle was already picked up
     */
    public boolean reservationDiscarded(UUID reservationId, DiscardReason reason) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            RentalAgreement agreement = agreements.forReservation(reservationId).orElseThrow(
                    () -> new InconsistentEventException("The reservation " + reservationId + " has no agreement"));
            if (!agreement.discard(reason)) {
                return false;
            }
            agreements.update(agreement);
            if (agreement.depositHeld()) {
                events.settleDeposit(agreement, Money.ofCents(0));
            }
            return true;
        }));
    }
}
