package pt.rentporto.rental.application.service;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.rental.application.port.RentalEvents;
import pt.rentporto.rental.domain.exception.FuelMismatchException;
import pt.rentporto.rental.domain.exception.InvalidRentalStatusException;
import pt.rentporto.rental.domain.exception.OdometerBelowPickupException;
import pt.rentporto.rental.domain.exception.RentalNotFoundException;
import pt.rentporto.rental.domain.exception.WrongReturnStationException;
import pt.rentporto.rental.domain.model.RentalAgreement;
import pt.rentporto.rental.domain.model.ReturnInspection;
import pt.rentporto.rental.domain.repository.RentalAgreements;

/**
 * Registers the return of a vehicle: the agreement is returned with its extra charges, the fleet hears of the vehicle
 * and the payments are asked to settle the deposit, all in one transaction.
 */
@Service
public class Returns {

    private final RentalAgreements agreements;
    private final RentalEvents events;
    private final Clock clock;
    private final TransactionTemplate transaction;

    Returns(
            RentalAgreements agreements,
            RentalEvents events,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.agreements = agreements;
        this.events = events;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Takes the vehicle of a rental back.
     *
     * @param rentalId   identifier of the agreement
     * @param station    code of the station where it is returned
     * @param inspection state of the vehicle
     * @return the returned agreement, with its extra charges
     * @throws RentalNotFoundException      if there is no agreement with the identifier
     * @throws InvalidRentalStatusException if the vehicle was not picked up or was already returned
     * @throws WrongReturnStationException  if the station is not the one agreed at booking
     * @throws OdometerBelowPickupException if the odometer is below its reading at pick-up
     * @throws FuelMismatchException        if the litres to fill do not match the fuel level
     */
    public RentalAgreement returnVehicle(UUID rentalId, String station, ReturnInspection inspection) {
        return transaction.execute(status -> {
            RentalAgreement agreement = agreements.lockWithId(rentalId)
                    .orElseThrow(() -> new RentalNotFoundException(rentalId));
            agreement.returnAt(station, inspection, clock.instant());
            agreements.update(agreement);
            events.vehicleReturned(agreement);
            events.settleDeposit(agreement, agreement.amountDue());
            return agreement;
        });
    }
}
