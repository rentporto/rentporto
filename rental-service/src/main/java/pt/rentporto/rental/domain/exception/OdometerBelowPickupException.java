package pt.rentporto.rental.domain.exception;

import pt.rentporto.web.error.BusinessException;

/**
 * The odometer at the return is below its reading at pick-up (422).
 */
public class OdometerBelowPickupException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param odometerKm       reading at the return
     * @param pickupOdometerKm reading at pick-up
     */
    public OdometerBelowPickupException(long odometerKm, long pickupOdometerKm) {
        super("ODOMETER_BELOW_PICKUP", Category.UNPROCESSABLE,
                "The odometer reads " + odometerKm + " km, fewer than the " + pickupOdometerKm + " km at pick-up");
    }
}
