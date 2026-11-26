package pt.rentporto.rental.application.port;

import java.util.UUID;
import pt.rentporto.rental.application.exception.FleetUnavailableException;
import pt.rentporto.rental.application.exception.NoVehicleAvailableException;

/**
 * Vehicles, assigned by the fleet-service. Implemented in the infrastructure.
 */
public interface Fleet {

    /**
     * Assigns a vehicle of a category at a station to a rental. Asking again for the same rental gets the same
     * vehicle.
     *
     * @param rentalId     the rental
     * @param categoryCode ACRISS code of the category booked
     * @param station      code of the pick-up station
     * @return the vehicle
     * @throws NoVehicleAvailableException if the station has no available vehicle of the category
     * @throws FleetUnavailableException   if the fleet did not answer
     */
    AssignedVehicle assign(UUID rentalId, String categoryCode, String station);

    /**
     * Vehicle assigned to a rental.
     *
     * @param licensePlate licence plate
     * @param odometerKm   odometer reading in kilometres
     */
    record AssignedVehicle(String licensePlate, long odometerKm) {
    }
}
