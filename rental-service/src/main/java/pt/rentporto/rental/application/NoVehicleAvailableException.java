package pt.rentporto.rental.application;

import pt.rentporto.web.BusinessException;

/**
 * The pick-up station has no available vehicle of the category booked (409).
 */
public class NoVehicleAvailableException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param station      code of the station
     * @param categoryCode ACRISS code of the category
     */
    public NoVehicleAvailableException(String station, String categoryCode) {
        super("NO_VEHICLE_AVAILABLE", Category.CONFLICT,
                "There is no available vehicle of the category " + categoryCode + " at " + station);
    }
}
