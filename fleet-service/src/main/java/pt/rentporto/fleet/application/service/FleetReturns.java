package pt.rentporto.fleet.application.service;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.application.port.FleetEvents;
import pt.rentporto.fleet.domain.exception.InconsistentReturnException;
import pt.rentporto.fleet.domain.exception.InvalidStatusTransitionException;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.StatusChange;
import pt.rentporto.fleet.domain.model.Vehicle;
import pt.rentporto.fleet.domain.model.VehicleAssignment;
import pt.rentporto.fleet.domain.model.VehicleStatus;
import pt.rentporto.fleet.domain.repository.Stations;
import pt.rentporto.fleet.domain.repository.StatusChanges;
import pt.rentporto.fleet.domain.repository.VehicleAssignments;
import pt.rentporto.fleet.domain.repository.Vehicles;

/**
 * Takes back the vehicles the customers return. The vehicle is the one assigned to the rental. A return at another
 * station moves the vehicle there: the capacity of the station it leaves is published before that of the station it
 * reaches, so whoever applies them in order never sees a vehicle too many.
 */
@Service
public class FleetReturns {

    static final String RETURNED_BY = "rental-service";

    private final Vehicles vehicles;
    private final VehicleAssignments assignments;
    private final Stations stations;
    private final StatusChanges history;
    private final FleetEvents events;
    private final Clock clock;
    private final TransactionTemplate transaction;

    FleetReturns(
            Vehicles vehicles,
            VehicleAssignments assignments,
            Stations stations,
            StatusChanges history,
            FleetEvents events,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.vehicles = vehicles;
        this.assignments = assignments;
        this.stations = stations;
        this.history = history;
        this.events = events;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Applies the return of the vehicle of a rental, once: a later return of the same rental changes nothing.
     *
     * @param rentalId         the rental
     * @param plate            licence plate of the vehicle returned
     * @param returnStation    code of the station where it was returned
     * @param odometerKm       odometer reading in kilometres
     * @param fuelLevelEighths fuel level in eighths of a tank
     * @return {@code true} if the return was applied, {@code false} if the vehicle had already been returned
     * @throws InconsistentReturnException      if the rental has no vehicle or another one, the station does not exist
     *                                          or the odometer went back
     * @throws InvalidStatusTransitionException if the vehicle is not rented
     */
    public boolean vehicleReturned(
            UUID rentalId, LicensePlate plate, String returnStation, long odometerKm, int fuelLevelEighths) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            assignments.lock(rentalId);
            VehicleAssignment assignment = assignments.forRental(rentalId).orElseThrow(
                    () -> new InconsistentReturnException("The rental " + rentalId + " has no vehicle"));
            if (!assignment.plate().equals(plate)) {
                throw new InconsistentReturnException("The rental " + rentalId + " has the vehicle "
                        + assignment.plate() + ", not " + plate);
            }
            if (assignment.returned()) {
                return false;
            }
            stations.withCode(returnStation).orElseThrow(
                    () -> new InconsistentReturnException("There is no station " + returnStation));
            Vehicle vehicle = vehicles.lockWithPlate(plate).orElseThrow(
                    () -> new InconsistentReturnException("There is no vehicle " + plate));
            String pickUpStation = vehicle.station();
            VehicleStatus before = vehicle.status();
            vehicle.returnAt(returnStation, odometerKm, fuelLevelEighths);
            assignment.close();
            vehicles.update(vehicle);
            assignments.update(assignment);
            history.add(StatusChange.of(plate, before, vehicle.status(), "Rental " + rentalId + " returned",
                    RETURNED_BY, clock.instant()));
            if (!pickUpStation.equals(returnStation)) {
                Stream.of(pickUpStation, returnStation).sorted().forEach(stations::lock);
                events.capacityChanged(vehicles.capacity(pickUpStation, vehicle.category()), plate);
                events.capacityChanged(vehicles.capacity(returnStation, vehicle.category()), plate);
            }
            return true;
        }));
    }
}
