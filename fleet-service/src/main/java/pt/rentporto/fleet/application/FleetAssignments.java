package pt.rentporto.fleet.application;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.NoVehicleAvailableException;
import pt.rentporto.fleet.domain.RentalAlreadyAssignedException;
import pt.rentporto.fleet.domain.Stations;
import pt.rentporto.fleet.domain.StatusChange;
import pt.rentporto.fleet.domain.StatusChanges;
import pt.rentporto.fleet.domain.UnknownCategoryException;
import pt.rentporto.fleet.domain.UnknownStationException;
import pt.rentporto.fleet.domain.Vehicle;
import pt.rentporto.fleet.domain.VehicleAssignment;
import pt.rentporto.fleet.domain.VehicleAssignments;
import pt.rentporto.fleet.domain.VehicleCategories;
import pt.rentporto.fleet.domain.VehicleStatus;
import pt.rentporto.fleet.domain.Vehicles;

/**
 * Assigns a vehicle of the booked category to a rental at pick-up: the one available the longest at the station,
 * locked while it is taken. Requests for the same rental happen one after the other, so a repeated request gets the
 * assignment of the first. A rented vehicle still counts in the capacity of its station, so no capacity is published.
 */
@Service
public class FleetAssignments {

    private final Vehicles vehicles;
    private final VehicleAssignments assignments;
    private final Stations stations;
    private final VehicleCategories categories;
    private final StatusChanges history;
    private final Clock clock;
    private final TransactionTemplate transaction;

    FleetAssignments(
            Vehicles vehicles,
            VehicleAssignments assignments,
            Stations stations,
            VehicleCategories categories,
            StatusChanges history,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.vehicles = vehicles;
        this.assignments = assignments;
        this.stations = stations;
        this.categories = categories;
        this.history = history;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Assigns a vehicle to a rental. A request that repeats the idempotency key of the one that made the assignment
     * gets that assignment.
     *
     * @param rentalId       the rental
     * @param category       ACRISS code of the booked category
     * @param station        code of the pick-up station
     * @param idempotencyKey key of the request
     * @param assignedBy     id of the agent at the counter
     * @return the assignment
     * @throws RentalAlreadyAssignedException if a request with another key assigned a vehicle to the rental
     * @throws UnknownStationException        if the station does not exist
     * @throws UnknownCategoryException       if the category does not exist
     * @throws NoVehicleAvailableException    if the station has no available vehicle of the category
     */
    public VehicleAssignment assign(
            UUID rentalId, AcrissCode category, String station, String idempotencyKey, String assignedBy) {
        return transaction.execute(status -> {
            assignments.lock(rentalId);
            return assignLocked(rentalId, category, station, idempotencyKey, assignedBy);
        });
    }

    private VehicleAssignment assignLocked(
            UUID rentalId, AcrissCode category, String station, String idempotencyKey, String assignedBy) {
        Optional<VehicleAssignment> existing = assignments.forRental(rentalId);
        if (existing.isPresent()) {
            if (existing.get().createdWith(idempotencyKey)) {
                return existing.get();
            }
            throw new RentalAlreadyAssignedException(rentalId);
        }
        stations.withCode(station).orElseThrow(() -> new UnknownStationException(station));
        categories.withCode(category).orElseThrow(() -> new UnknownCategoryException(category));
        Vehicle vehicle = vehicles.lockFirstAvailable(station, category)
                .orElseThrow(() -> new NoVehicleAvailableException(station, category));
        VehicleStatus before = vehicle.status();
        VehicleAssignment assignment = vehicle.assignTo(rentalId, idempotencyKey);
        vehicles.update(vehicle);
        assignments.add(assignment);
        history.add(StatusChange.of(
                vehicle.plate(), before, vehicle.status(), "Rental " + rentalId, assignedBy, clock.instant()));
        return assignment;
    }
}
