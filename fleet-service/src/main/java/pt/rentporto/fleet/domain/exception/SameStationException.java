package pt.rentporto.fleet.domain.exception;

import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.web.error.BusinessException;

/**
 * A vehicle cannot be transferred to the station where it already is (422).
 */
public class SameStationException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param plate   licence plate of the vehicle
     * @param station code of the station
     */
    public SameStationException(LicensePlate plate, String station) {
        super("SAME_STATION", Category.UNPROCESSABLE, "The vehicle " + plate + " is already at " + station);
    }
}
