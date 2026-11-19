package pt.rentporto.fleet.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Stored assignments of vehicles to rentals. Implemented in the infrastructure.
 */
public interface VehicleAssignments {

    /**
     * Locks a rental until the end of the transaction, so that two requests for it happen one after the other: a
     * repeated request waits for the first one and then finds its assignment, instead of competing with it for the
     * vehicles.
     *
     * @param rentalId the rental
     */
    void lock(UUID rentalId);

    /**
     * Finds the assignment of a rental.
     *
     * @param rentalId the rental
     * @return the assignment, if there is one
     */
    Optional<VehicleAssignment> forRental(UUID rentalId);

    /**
     * Stores a new assignment.
     *
     * @param assignment the assignment
     * @throws RentalAlreadyAssignedException if an assignment of the same rental was stored first
     */
    void add(VehicleAssignment assignment);

    /**
     * Stores the changes to an assignment.
     *
     * @param assignment the assignment
     */
    void update(VehicleAssignment assignment);
}
