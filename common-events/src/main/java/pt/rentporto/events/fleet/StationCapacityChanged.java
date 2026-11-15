package pt.rentporto.events.fleet;

import java.util.Objects;

/**
 * The number of vehicles of a category that a station can deliver changed.
 *
 * <p>Published by the fleet-service on {@code fleet.station-capacity-changed}; consumed by the reservation module of
 * the booking-service.
 *
 * @param stationCode  station code, e.g. {@code OPO}
 * @param categoryCode ACRISS code of the vehicle category, e.g. {@code ECMR}
 * @param capacity     new capacity; replaces the previous value
 */
public record StationCapacityChanged(String stationCode, String categoryCode, int capacity) {

    public StationCapacityChanged {
        Objects.requireNonNull(stationCode, "stationCode");
        Objects.requireNonNull(categoryCode, "categoryCode");
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0, was " + capacity);
        }
    }
}
