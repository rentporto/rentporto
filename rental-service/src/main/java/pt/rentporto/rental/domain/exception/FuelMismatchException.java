package pt.rentporto.rental.domain.exception;

import java.math.BigDecimal;
import pt.rentporto.web.error.BusinessException;

/**
 * The litres to fill do not match the fuel level: a tank that is not full needs litres, and a full one none (422).
 */
public class FuelMismatchException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param fuelLevelEighths fuel level found
     * @param litresToFill     litres put in
     */
    public FuelMismatchException(int fuelLevelEighths, BigDecimal litresToFill) {
        super("FUEL_MISMATCH", Category.UNPROCESSABLE,
                "A tank at " + fuelLevelEighths + "/8 cannot take " + litresToFill.toPlainString() + " litres");
    }
}
