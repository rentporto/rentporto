package pt.rentporto.booking.pricing.api.model;

/**
 * How often a fee is charged.
 */
public enum FeeBasis {
    /** Charged for every 24-hour day of the rental. */
    DAY,
    /** Charged once per rental. */
    RENTAL
}
