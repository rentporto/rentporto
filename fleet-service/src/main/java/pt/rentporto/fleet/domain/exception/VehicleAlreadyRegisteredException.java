package pt.rentporto.fleet.domain.exception;

import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.web.error.BusinessException;

/**
 * A vehicle with the same licence plate is already registered (409).
 */
public class VehicleAlreadyRegisteredException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param plate licence plate of the vehicle
     */
    public VehicleAlreadyRegisteredException(LicensePlate plate) {
        super("VEHICLE_ALREADY_REGISTERED", Category.CONFLICT, "The vehicle " + plate + " is already registered");
    }
}
