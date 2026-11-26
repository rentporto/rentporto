package pt.rentporto.rental.domain.exception;

import pt.rentporto.web.error.BusinessException;

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
