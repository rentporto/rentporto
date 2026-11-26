package pt.rentporto.fleet.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * State of a vehicle in the fleet, with the transitions between states and the states that count in the capacity of
 * a station.
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
    RETIRED;

    /**
     * Tells whether a vehicle in this state can move to another one: an available vehicle is rented, sent to
     * maintenance, transferred or retired; a rented one comes back on return; one in maintenance comes back or is
     * retired; one in transfer arrives available; a retired one never changes.
     *
     * @param next the state to move to
     * @return {@code true} if the transition is allowed
     */
    public boolean canChangeTo(VehicleStatus next) {
        return switch (this) {
            case AVAILABLE -> next == RENTED || next == IN_MAINTENANCE || next == IN_TRANSFER || next == RETIRED;
            case RENTED, IN_TRANSFER -> next == AVAILABLE;
            case IN_MAINTENANCE -> next == AVAILABLE || next == RETIRED;
            case RETIRED -> false;
        };
    }

    /**
     * The states that count in the capacity of the station where the vehicle is. A rented vehicle still counts: its
     * booking already takes a place at the pick-up station for the whole rental, and it only changes station when it
     * is returned.
     *
     * @return {@code AVAILABLE} and {@code RENTED}
     */
    public static Set<VehicleStatus> countedInCapacity() {
        return EnumSet.of(AVAILABLE, RENTED);
    }
}
