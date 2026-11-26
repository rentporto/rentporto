package pt.rentporto.fleet.domain.exception;

import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.web.error.BusinessException;

/**
 * There is no vehicle with the licence plate (404).
 */
public class VehicleNotFoundException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param plate licence plate of the vehicle
     */
    public VehicleNotFoundException(LicensePlate plate) {
        super("VEHICLE_NOT_FOUND", Category.NOT_FOUND, "There is no vehicle " + plate);
    }
}
