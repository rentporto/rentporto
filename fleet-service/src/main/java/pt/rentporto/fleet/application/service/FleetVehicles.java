package pt.rentporto.fleet.application.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.application.port.FleetEvents;
import pt.rentporto.fleet.domain.exception.InvalidStatusTransitionException;
import pt.rentporto.fleet.domain.exception.UnknownCategoryException;
import pt.rentporto.fleet.domain.exception.UnknownStationException;
import pt.rentporto.fleet.domain.exception.VehicleAlreadyRegisteredException;
import pt.rentporto.fleet.domain.exception.VehicleNotFoundException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.StatusChange;
import pt.rentporto.fleet.domain.model.Vehicle;
import pt.rentporto.fleet.domain.model.VehicleStatus;
import pt.rentporto.fleet.domain.repository.Stations;
import pt.rentporto.fleet.domain.repository.StatusChanges;
import pt.rentporto.fleet.domain.repository.VehicleCategories;
import pt.rentporto.fleet.domain.repository.Vehicles;

/**
 * Registers the vehicles of the fleet and changes their state. Every change is written to the history, and a change
 * to the capacity of a station publishes {@code StationCapacityChanged} in the same transaction, counted with the
 * station locked.
 */
@Service
public class FleetVehicles {

    private final Vehicles vehicles;
    private final Stations stations;
    private final VehicleCategories categories;
    private final StatusChanges history;
    private final FleetEvents events;
    private final Clock clock;
    private final TransactionTemplate transaction;

    FleetVehicles(
            Vehicles vehicles,
            Stations stations,
            VehicleCategories categories,
            StatusChanges history,
            FleetEvents events,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.vehicles = vehicles;
        this.stations = stations;
        this.categories = categories;
        this.history = history;
        this.events = events;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Registers a vehicle, available at its station, and publishes the new capacity of the station. A request that
     * repeats the idempotency key of the one that registered the vehicle gets that vehicle, without publishing again.
     *
     * @param plate          licence plate
     * @param category       ACRISS code of its category
     * @param station        code of the station where it is
     * @param odometerKm     odometer reading in kilometres
     * @param idempotencyKey key of the request, or {@code null}
     * @param registeredBy   id of the user who registers it
     * @return the vehicle
     * @throws VehicleAlreadyRegisteredException if another request registered the plate
     * @throws UnknownStationException           if the station does not exist
     * @throws UnknownCategoryException          if the category does not exist
     */
    public Vehicle register(
            LicensePlate plate,
            AcrissCode category,
            String station,
            long odometerKm,
            String idempotencyKey,
            String registeredBy) {
        try {
            return transaction.execute(
                    status -> registerOnce(plate, category, station, odometerKm, idempotencyKey, registeredBy));
        } catch (VehicleAlreadyRegisteredException e) {
            return transaction.execute(
                    status -> registerOnce(plate, category, station, odometerKm, idempotencyKey, registeredBy));
        }
    }

    /**
     * Lists the vehicles of a station and in a state.
     *
     * @param station code of the station, or {@code null} for every station
     * @param status  state, or {@code null} for every state
     * @return the vehicles that match, by licence plate
     */
    public List<Vehicle> vehicles(String station, VehicleStatus status) {
        return vehicles.matching(station, status);
    }

    /**
     * Changes the state of a vehicle as a fleet manager asks. Asking for the state it is in changes nothing.
     *
     * @param plate     licence plate
     * @param target    state asked for
     * @param reason    why, or {@code null}
     * @param changedBy id of the user who asks
     * @return the vehicle
     * @throws VehicleNotFoundException         if there is no vehicle with the plate
     * @throws InvalidStatusTransitionException if a fleet manager cannot move the vehicle to that state
     */
    public Vehicle changeStatus(LicensePlate plate, VehicleStatus target, String reason, String changedBy) {
        return transaction.execute(status -> {
            Vehicle vehicle = vehicles.lockWithPlate(plate).orElseThrow(() -> new VehicleNotFoundException(plate));
            VehicleStatus before = vehicle.status();
            boolean counted = vehicle.countsInCapacity();
            if (!vehicle.changeStatus(target)) {
                return vehicle;
            }
            vehicles.update(vehicle);
            history.add(StatusChange.of(plate, before, vehicle.status(), reason, changedBy, clock.instant()));
            if (counted != vehicle.countsInCapacity()) {
                stations.lock(vehicle.station());
                publishCapacity(vehicle);
            }
            return vehicle;
        });
    }

    private Vehicle registerOnce(
            LicensePlate plate,
            AcrissCode category,
            String station,
            long odometerKm,
            String idempotencyKey,
            String registeredBy) {
        Optional<Vehicle> existing = vehicles.withPlate(plate);
        if (existing.isPresent()) {
            if (existing.get().registeredWith(idempotencyKey)) {
                return existing.get();
            }
            throw new VehicleAlreadyRegisteredException(plate);
        }
        if (categories.withCode(category).isEmpty()) {
            throw new UnknownCategoryException(category);
        }
        stations.lock(station).orElseThrow(() -> new UnknownStationException(station));
        Vehicle vehicle = Vehicle.register(plate, category, station, odometerKm, idempotencyKey);
        vehicles.add(vehicle);
        history.add(StatusChange.of(plate, null, vehicle.status(), null, registeredBy, clock.instant()));
        publishCapacity(vehicle);
        return vehicle;
    }

    private void publishCapacity(Vehicle vehicle) {
        events.capacityChanged(vehicles.capacity(vehicle.station(), vehicle.category()), vehicle.plate());
    }
}
