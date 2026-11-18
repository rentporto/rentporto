package pt.rentporto.fleet.application;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.domain.InvalidStatusTransitionException;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.SameStationException;
import pt.rentporto.fleet.domain.Stations;
import pt.rentporto.fleet.domain.StatusChange;
import pt.rentporto.fleet.domain.StatusChanges;
import pt.rentporto.fleet.domain.Transfer;
import pt.rentporto.fleet.domain.TransferNotFoundException;
import pt.rentporto.fleet.domain.Transfers;
import pt.rentporto.fleet.domain.UnknownStationException;
import pt.rentporto.fleet.domain.Vehicle;
import pt.rentporto.fleet.domain.VehicleNotFoundException;
import pt.rentporto.fleet.domain.VehicleStatus;
import pt.rentporto.fleet.domain.Vehicles;

/**
 * Transfers vehicles between stations. A vehicle in transfer counts at no station, so the origin loses it when the
 * transfer starts and the destination gains it when it is completed; each step publishes the new capacity of the one
 * station that changed, counted with that station locked. The vehicle is locked first, so two requests about it
 * happen one after the other.
 */
@Service
public class FleetTransfers {

    private final Vehicles vehicles;
    private final Stations stations;
    private final Transfers transfers;
    private final StatusChanges history;
    private final FleetEvents events;
    private final Clock clock;
    private final TransactionTemplate transaction;

    FleetTransfers(
            Vehicles vehicles,
            Stations stations,
            Transfers transfers,
            StatusChanges history,
            FleetEvents events,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.vehicles = vehicles;
        this.stations = stations;
        this.transfers = transfers;
        this.history = history;
        this.events = events;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Starts the transfer of a vehicle to another station. A request that repeats the idempotency key of the one that
     * started a transfer of the vehicle gets that transfer.
     *
     * @param plate          licence plate of the vehicle
     * @param destination    code of the destination station
     * @param idempotencyKey key of the request, or {@code null}
     * @param startedBy      id of the user who starts it
     * @return the transfer
     * @throws VehicleNotFoundException         if there is no vehicle with the plate
     * @throws UnknownStationException          if the destination does not exist
     * @throws InvalidStatusTransitionException if the vehicle is not available
     * @throws SameStationException             if the vehicle is already at the destination
     */
    public Transfer start(LicensePlate plate, String destination, String idempotencyKey, String startedBy) {
        return transaction.execute(status -> {
            Vehicle vehicle = vehicles.lockWithPlate(plate).orElseThrow(() -> new VehicleNotFoundException(plate));
            if (idempotencyKey != null) {
                Optional<Transfer> existing = transfers.startedWith(plate, idempotencyKey);
                if (existing.isPresent()) {
                    return existing.get();
                }
            }
            stations.withCode(destination).orElseThrow(() -> new UnknownStationException(destination));
            VehicleStatus before = vehicle.status();
            Transfer transfer = vehicle.startTransfer(destination, idempotencyKey);
            vehicles.update(vehicle);
            transfers.add(transfer);
            history.add(StatusChange.of(
                    plate, before, vehicle.status(), "Transfer " + transfer.id(), startedBy, clock.instant()));
            publishCapacity(vehicle);
            return transfer;
        });
    }

    /**
     * Completes a transfer when the vehicle arrives. Completing a transfer that is already completed changes nothing.
     *
     * @param plate       licence plate of the vehicle
     * @param id          transfer identifier
     * @param completedBy id of the user who completes it
     * @return the transfer
     * @throws TransferNotFoundException if the vehicle has no transfer with the identifier
     */
    public Transfer complete(LicensePlate plate, UUID id, String completedBy) {
        return transaction.execute(status -> {
            Vehicle vehicle = vehicles.lockWithPlate(plate)
                    .orElseThrow(() -> new TransferNotFoundException(plate, id));
            Transfer transfer = transfers.withId(id)
                    .filter(found -> found.plate().equals(plate))
                    .orElseThrow(() -> new TransferNotFoundException(plate, id));
            if (!transfer.complete()) {
                return transfer;
            }
            VehicleStatus before = vehicle.status();
            vehicle.arriveAt(transfer.toStation());
            vehicles.update(vehicle);
            transfers.update(transfer);
            history.add(StatusChange.of(
                    plate, before, vehicle.status(), "Transfer " + transfer.id(), completedBy, clock.instant()));
            publishCapacity(vehicle);
            return transfer;
        });
    }

    private void publishCapacity(Vehicle vehicle) {
        stations.lock(vehicle.station());
        events.capacityChanged(vehicles.capacity(vehicle.station(), vehicle.category()), vehicle.plate());
    }
}
