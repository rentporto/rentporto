package pt.rentporto.rental.domain;

import pt.rentporto.web.BusinessException;

/**
 * The vehicle is returned at another station than the one agreed at booking (422).
 */
public class WrongReturnStationException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param station station of the return
     * @param agreed  station agreed at booking
     */
    public WrongReturnStationException(String station, String agreed) {
        super("WRONG_RETURN_STATION", Category.UNPROCESSABLE,
                "The vehicle must be returned at " + agreed + ", not at " + station);
    }
}
