package pt.rentporto.rental.application;

import pt.rentporto.rental.domain.RentalAgreement;

/**
 * Events and commands of the rental context for the other services, written in the transaction of the change.
 * Implemented in the infrastructure.
 */
public interface RentalEvents {

    /**
     * Publishes {@code RentalStarted}: the customer picked up the vehicle.
     *
     * @param agreement the active agreement
     */
    void rentalStarted(RentalAgreement agreement);

    /**
     * Sends {@code SettleDeposit} with nothing due, which releases the deposit of an agreement that will not start.
     *
     * @param agreement the discarded agreement
     */
    void depositReleased(RentalAgreement agreement);
}
