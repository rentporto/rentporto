package pt.rentporto.fleet.domain.model;

import java.util.Objects;

/**
 * Number of vehicles of a category that a station can deliver: those that count in its capacity, see
 * {@link VehicleStatus#countedInCapacity()}.
 *
 * @param station  code of the station
 * @param category ACRISS code of the category
 * @param vehicles number of vehicles
 */
public record StationCapacity(String station, AcrissCode category, int vehicles) {

    public StationCapacity {
        Objects.requireNonNull(station, "station");
        Objects.requireNonNull(category, "category");
        if (vehicles < 0) {
            throw new IllegalArgumentException("vehicles must be >= 0, was " + vehicles);
        }
    }
}
