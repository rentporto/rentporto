package pt.rentporto.rental.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.rental.application.exception.DepositDeclinedException;
import pt.rentporto.rental.application.exception.FleetUnavailableException;
import pt.rentporto.rental.application.exception.NoVehicleAvailableException;
import pt.rentporto.rental.application.exception.PaymentUnavailableException;
import pt.rentporto.rental.application.port.Deposits;
import pt.rentporto.rental.application.port.Fleet;
import pt.rentporto.rental.application.port.RentalEvents;
import pt.rentporto.rental.domain.exception.DriverLicenseNotValidException;
import pt.rentporto.rental.domain.exception.InvalidRentalStatusException;
import pt.rentporto.rental.domain.exception.RentalNotFoundException;
import pt.rentporto.rental.domain.model.AdditionalDriver;
import pt.rentporto.rental.domain.model.RentalAgreement;
import pt.rentporto.rental.domain.repository.RentalAgreements;

/**
 * Registers the pick-up while the customer waits at the counter. The deposit comes first, so a refused card never
 * leaves a vehicle assigned; the deposit is recorded as soon as it is held, and the deposit and the vehicle are asked
 * with keys of the rental, so a pick-up tried again after a failure goes on from where it stopped.
 */
@Service
public class PickUps {

    private final RentalAgreements agreements;
    private final Deposits deposits;
    private final Fleet fleet;
    private final RentalEvents events;
    private final Clock clock;
    private final TransactionTemplate transaction;

    PickUps(
            RentalAgreements agreements,
            Deposits deposits,
            Fleet fleet,
            RentalEvents events,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.agreements = agreements;
        this.deposits = deposits;
        this.fleet = fleet;
        this.events = events;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Picks up the vehicle of a rental: holds the deposit, gets a vehicle of the category booked and makes the
     * agreement active, publishing {@code RentalStarted}.
     *
     * @param rentalId identifier of the agreement
     * @param drivers  additional drivers, possibly none
     * @param notes    notes of the check-out inspection, or {@code null}
     * @return the active agreement
     * @throws RentalNotFoundException        if there is no agreement with the identifier
     * @throws InvalidRentalStatusException   if the agreement is not waiting for the pick-up
     * @throws DriverLicenseNotValidException if the licence of an additional driver cannot be used today
     * @throws DepositDeclinedException       if the card refused the deposit
     * @throws PaymentUnavailableException    if the payments did not answer
     * @throws NoVehicleAvailableException    if the station has no available vehicle of the category
     * @throws FleetUnavailableException      if the fleet did not answer
     */
    public RentalAgreement pickUp(UUID rentalId, List<AdditionalDriver> drivers, String notes) {
        LocalDate today = LocalDate.now(clock);
        drivers.forEach(driver -> driver.requireValidLicenseOn(today));
        RentalAgreement agreement =
                agreements.withId(rentalId).orElseThrow(() -> new RentalNotFoundException(rentalId));
        agreement.requireAwaitingPickup();
        if (!agreement.depositHeld()) {
            UUID depositId = deposits.authorize(rentalId, agreement.reservationId(), agreement.terms().deposit());
            transaction.executeWithoutResult(status -> {
                RentalAgreement locked = lock(rentalId);
                locked.recordDeposit(depositId);
                agreements.update(locked);
            });
        }
        Fleet.AssignedVehicle vehicle = fleet.assign(rentalId, agreement.categoryCode(), agreement.pickupStation());
        return transaction.execute(status -> {
            RentalAgreement locked = lock(rentalId);
            locked.pickUp(vehicle.licensePlate(), vehicle.odometerKm(), drivers, notes, clock.instant(), today);
            agreements.update(locked);
            events.rentalStarted(locked);
            return locked;
        });
    }

    private RentalAgreement lock(UUID rentalId) {
        return agreements.lockWithId(rentalId).orElseThrow(() -> new RentalNotFoundException(rentalId));
    }
}
