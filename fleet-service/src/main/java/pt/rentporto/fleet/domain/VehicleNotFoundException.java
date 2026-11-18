package pt.rentporto.fleet.domain;

import pt.rentporto.web.BusinessException;

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
