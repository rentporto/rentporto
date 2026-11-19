package pt.rentporto.rental.domain;

/**
 * Status of a rental agreement.
 */
public enum RentalStatus {

    /** Created when the reservation was confirmed; waiting for the customer. */
    AWAITING_PICKUP,
    /** The customer has the vehicle. */
    ACTIVE,
    /** The vehicle was returned; the deposit is being settled. */
    RETURNED,
    /** Settled and closed. */
    CLOSED,
    /** The reservation was cancelled or not picked up, so the agreement never started. */
    DISCARDED
}
