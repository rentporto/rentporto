package pt.rentporto.fleet.api;

import pt.rentporto.fleet.domain.Station;

/**
 * Station where vehicles are picked up and returned.
 *
 * @param code station code, e.g. {@code OPO}
 * @param name station name, e.g. {@code Porto Airport}
 */
public record StationResponse(String code, String name) {

    static StationResponse from(Station station) {
        return new StationResponse(station.code(), station.name());
    }
}
