package pt.rentporto.fleet.domain.exception;

import java.util.UUID;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.web.error.BusinessException;

/**
 * The vehicle has no transfer with the identifier (404).
 */
public class TransferNotFoundException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param plate licence plate of the vehicle
     * @param id    transfer identifier
     */
    public TransferNotFoundException(LicensePlate plate, UUID id) {
        super("TRANSFER_NOT_FOUND", Category.NOT_FOUND, "The vehicle " + plate + " has no transfer " + id);
    }
}
