package pt.rentporto.fleet.api;

/**
 * State of a vehicle in the fleet.
 */
public enum VehicleStatus {
    /** Ready to be assigned at its station. */
    AVAILABLE,
    /** With a customer. */
    RENTED,
    /** Out of service for maintenance. */
    IN_MAINTENANCE,
    /** Moving between stations. */
    IN_TRANSFER,
    /** Permanently out of the fleet. */
    RETIRED
}
