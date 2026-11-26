package pt.rentporto.fleet.domain.exception;

import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.web.error.BusinessException;

/**
 * The station has no available vehicle of the category (409).
 */
public class NoVehicleAvailableException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param station  code of the station
     * @param category ACRISS code of the category
     */
    public NoVehicleAvailableException(String station, AcrissCode category) {
        super("NO_VEHICLE_AVAILABLE", Category.CONFLICT,
                "There is no available vehicle of the category " + category + " at " + station);
    }
}
