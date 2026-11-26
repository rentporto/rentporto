package pt.rentporto.fleet.application.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pt.rentporto.fleet.domain.exception.VehicleAlreadyRegisteredException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.StationCapacity;
import pt.rentporto.fleet.domain.model.Vehicle;
import pt.rentporto.fleet.domain.model.VehicleStatus;
import pt.rentporto.fleet.domain.repository.Vehicles;

/**
 * Vehicles in memory. {@code registeredMeanwhile} plays another request that registers the same plate just before
 * this one.
 */
final class InMemoryVehicles implements Vehicles {

    private final Map<LicensePlate, Vehicle> byPlate = new HashMap<>();
    Vehicle registeredMeanwhile;

    @Override
    public Optional<Vehicle> withPlate(LicensePlate plate) {
        return Optional.ofNullable(byPlate.get(plate));
    }

    @Override
    public Optional<Vehicle> lockWithPlate(LicensePlate plate) {
        return withPlate(plate);
    }

    @Override
    public Optional<Vehicle> lockFirstAvailable(String station, AcrissCode category) {
        return matching(station, VehicleStatus.AVAILABLE).stream()
                .filter(vehicle -> vehicle.category().equals(category))
                .findFirst();
    }

    @Override
    public List<Vehicle> matching(String station, VehicleStatus status) {
        return byPlate.values().stream()
                .filter(vehicle -> station == null || vehicle.station().equals(station))
                .filter(vehicle -> status == null || vehicle.status() == status)
                .sorted(Comparator.comparing(vehicle -> vehicle.plate().value()))
                .toList();
    }

    @Override
    public StationCapacity capacity(String station, AcrissCode category) {
        long count = byPlate.values().stream()
                .filter(vehicle -> vehicle.station().equals(station) && vehicle.category().equals(category))
                .filter(Vehicle::countsInCapacity)
                .count();
        return new StationCapacity(station, category, (int) count);
    }

    @Override
    public void add(Vehicle vehicle) {
        if (registeredMeanwhile != null && registeredMeanwhile.plate().equals(vehicle.plate())) {
            byPlate.put(registeredMeanwhile.plate(), registeredMeanwhile);
            registeredMeanwhile = null;
            throw new VehicleAlreadyRegisteredException(vehicle.plate());
        }
        byPlate.put(vehicle.plate(), vehicle);
    }

    @Override
    public void update(Vehicle vehicle) {
        byPlate.put(vehicle.plate(), vehicle);
    }
}
