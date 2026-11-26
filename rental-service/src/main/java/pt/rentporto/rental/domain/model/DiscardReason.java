package pt.rentporto.rental.domain.model;

/**
 * Why a rental agreement was discarded before the pick-up.
 */
public enum DiscardReason {

    /** The customer cancelled the reservation. */
    CANCELLED,
    /** The customer did not pick up the vehicle. */
    NO_SHOW
}
