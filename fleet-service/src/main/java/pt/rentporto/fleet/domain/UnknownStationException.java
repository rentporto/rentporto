package pt.rentporto.fleet.domain;

import pt.rentporto.web.BusinessException;

/**
 * A vehicle refers to a station that does not exist (422).
 */
public class UnknownStationException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param code code of the station
     */
    public UnknownStationException(String code) {
        super("UNKNOWN_STATION", Category.UNPROCESSABLE, "There is no station " + code);
    }
}
