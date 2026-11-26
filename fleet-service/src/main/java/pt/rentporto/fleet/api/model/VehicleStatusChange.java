package pt.rentporto.fleet.api.model;

/**
 * State a fleet manager can set directly; the other states come from rentals and transfers.
 */
public enum VehicleStatusChange {
    /** Back in service. */
    AVAILABLE,
    /** Out of service for maintenance. */
    IN_MAINTENANCE,
    /** Permanently out of the fleet. */
    RETIRED
}
