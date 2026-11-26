package pt.rentporto.fleet.api.response;

import pt.rentporto.fleet.domain.model.Station;

/**
 * Station where vehicles are picked up and returned.
 *
 * @param code station code, e.g. {@code OPO}
 * @param name station name, e.g. {@code Porto Airport}
 */
public record StationResponse(String code, String name) {

    public static StationResponse from(Station station) {
        return new StationResponse(station.code(), station.name());
    }
}
