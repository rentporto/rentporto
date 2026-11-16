package pt.rentporto.fleet.api;

/**
 * Station where vehicles are picked up and returned.
 *
 * @param code station code, e.g. {@code OPO}
 * @param name station name, e.g. {@code Porto Airport}
 */
public record StationResponse(String code, String name) {
}
