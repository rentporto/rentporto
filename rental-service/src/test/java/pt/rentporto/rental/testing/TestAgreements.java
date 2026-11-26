package pt.rentporto.rental.testing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.rental.application.service.PendingAgreements;
import pt.rentporto.rental.domain.model.ConfirmedReservation;
import pt.rentporto.rental.domain.model.RentalAgreement;
import pt.rentporto.rental.domain.model.ReturnInspection;
import pt.rentporto.rental.domain.repository.RentalAgreements;

/**
 * Stores agreements in each state for the tests, through the domain and without the other services: one confirmed
 * reservation from the airport to Matosinhos, picked up with the vehicle AA-12-BB and returned full.
 */
public class TestAgreements {

    private final PendingAgreements pending;
    private final RentalAgreements agreements;
    private final TransactionTemplate transaction;
    private final Clock clock;

    /**
     * Creates the helper.
     *
     * @param pending            creates the agreements of confirmed reservations
     * @param agreements         stored agreements
     * @param transactionManager transactions of the service
     * @param clock              clock of the service
     */
    public TestAgreements(
            PendingAgreements pending,
            RentalAgreements agreements,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.pending = pending;
        this.agreements = agreements;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * An agreement waiting for the pick-up, returned in an hour.
     *
     * @return the agreement
     */
    public RentalAgreement awaitingPickup() {
        return awaitingPickup(clock.instant().plusSeconds(3600));
    }

    /**
     * An agreement waiting for the pick-up, for three days up to a return.
     *
     * @param returnAt the return agreed
     * @return the agreement
     */
    public RentalAgreement awaitingPickup(Instant returnAt) {
        ConfirmedReservation reservation = Reservations.returningAt(returnAt);
        pending.reservationConfirmed(reservation);
        return agreements.forReservation(reservation.reservationId()).orElseThrow();
    }

    /**
     * An agreement picked up, with a deposit held.
     *
     * @return the agreement
     */
    public RentalAgreement active() {
        UUID rentalId = awaitingPickup().id();
        return change(rentalId, agreement -> {
            agreement.recordDeposit(UUID.randomUUID());
            agreement.pickUp("AA-12-BB", 12000, List.of(), null, clock.instant(), LocalDate.now(clock));
        });
    }

    /**
     * An agreement returned on time and full, with nothing to charge.
     *
     * @return the agreement
     */
    public RentalAgreement returned() {
        UUID rentalId = active().id();
        return change(rentalId, agreement -> agreement.returnAt(
                "MAT", new ReturnInspection(12450, 8, BigDecimal.ZERO, List.of(), null), clock.instant()));
    }

    private RentalAgreement change(UUID rentalId, Consumer<RentalAgreement> change) {
        return transaction.execute(status -> {
            RentalAgreement agreement = agreements.lockWithId(rentalId).orElseThrow();
            change.accept(agreement);
            agreements.update(agreement);
            return agreement;
        });
    }
}
