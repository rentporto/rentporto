package pt.rentporto.fleet.domain.exception;

import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.VehicleStatus;
import pt.rentporto.web.error.BusinessException;

/**
 * The vehicle cannot move from its state to the one asked for (409).
 */
public class InvalidStatusTransitionException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param plate licence plate of the vehicle
     * @param from  current state
     * @param to    state asked for
     */
    public InvalidStatusTransitionException(LicensePlate plate, VehicleStatus from, VehicleStatus to) {
        super("INVALID_STATUS_TRANSITION", Category.CONFLICT,
                "The vehicle " + plate + " cannot go from " + from + " to " + to);
    }
}
