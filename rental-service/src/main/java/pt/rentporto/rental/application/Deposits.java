package pt.rentporto.rental.application;

import java.util.UUID;
import pt.rentporto.rental.domain.Money;

/**
 * Security deposits, held by the payment module of the booking-service. Implemented in the infrastructure.
 */
public interface Deposits {

    /**
     * Pre-authorises the security deposit of a rental on the card of the advance payment. Asking again for the same
     * rental gets the same deposit.
     *
     * @param rentalId      the rental
     * @param reservationId the reservation paid in advance
     * @param amount        amount to pre-authorise
     * @return identifier of the deposit
     * @throws DepositDeclinedException    if the card refused it
     * @throws PaymentUnavailableException if the payments did not answer
     */
    UUID authorize(UUID rentalId, UUID reservationId, Money amount);
}
