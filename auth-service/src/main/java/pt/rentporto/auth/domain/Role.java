package pt.rentporto.auth.domain;

/**
 * Role of a user account, carried in the {@code roles} claim of the JWT.
 */
public enum Role {
    /** Customer who books and rents vehicles. */
    CUSTOMER,
    /** Counter agent who handles pick-ups and returns. */
    AGENT,
    /** Manages stations, categories and vehicles. */
    FLEET_MANAGER,
    /** Manages rates, seasons, extras, coverages and fees. */
    PRICING_MANAGER,
    /** Manages the staff accounts. */
    ADMIN
}
