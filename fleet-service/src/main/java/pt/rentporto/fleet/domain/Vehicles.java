package pt.rentporto.fleet.domain;

import java.util.List;
import java.util.Optional;

/**
 * Stored vehicles. Implemented in the infrastructure.
 */
public interface Vehicles {

    /**
     * Finds a vehicle.
     *
     * @param plate licence plate
     * @return the vehicle, if there is one
     */
    Optional<Vehicle> withPlate(LicensePlate plate);

    /**
     * Finds a vehicle and locks it until the end of the transaction, so that two changes to it happen one after the
     * other.
     *
     * @param plate licence plate
     * @return the vehicle, if there is one
     */
    Optional<Vehicle> lockWithPlate(LicensePlate plate);

    /**
     * Lists the vehicles of a station and in a state.
     *
     * @param station code of the station, or {@code null} for every station
     * @param status  state, or {@code null} for every state
     * @return the vehicles that match, by licence plate
     */
    List<Vehicle> matching(String station, VehicleStatus status);

    /**
     * Counts the vehicles of a category at a station that count in its capacity.
     *
     * @param station  code of the station
     * @param category ACRISS code of the category
     * @return the capacity
     */
    StationCapacity capacity(String station, AcrissCode category);

    /**
     * Stores a new vehicle.
     *
     * @param vehicle the vehicle
     * @throws VehicleAlreadyRegisteredException if a vehicle with the same plate was stored first
     */
    void add(Vehicle vehicle);

    /**
     * Stores the changes to a vehicle.
     *
     * @param vehicle the vehicle
     */
    void update(Vehicle vehicle);
}
