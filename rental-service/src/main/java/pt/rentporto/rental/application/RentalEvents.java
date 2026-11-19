package pt.rentporto.rental.application;

import pt.rentporto.rental.domain.Money;
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
     * Publishes {@code VehicleReturned}: the vehicle is back, with its odometer, its fuel and its new damages.
     *
     * @param agreement the returned agreement
     */
    void vehicleReturned(RentalAgreement agreement);

    /**
     * Sends {@code SettleDeposit}: the payments capture what is due from the deposit of the agreement and release the
     * rest; with nothing due, the whole deposit is released.
     *
     * @param agreement the agreement whose deposit is settled
     * @param amountDue what the customer owes
     */
    void settleDeposit(RentalAgreement agreement, Money amountDue);
}
