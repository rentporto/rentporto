package pt.rentporto.fleet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import pt.rentporto.fleet.domain.exception.InvalidLicensePlateException;

/**
 * Portuguese licence plate, which identifies a vehicle. The fleet has plates of the three formats issued since 1992:
 * {@code AA-00-AA} (since 2020), {@code 00-AA-00} (2005 to 2020) and {@code 00-00-AA} (1992 to 2005).
 *
 * @param value the plate, e.g. {@code AA-12-BB}
 */
@Embeddable
public record LicensePlate(@Column(name = "license_plate", nullable = false) String value) {

    /**
     * The three formats, for the validation of the API.
     */
    public static final String FORMAT = "[A-Z]{2}-\\d{2}-[A-Z]{2}|\\d{2}-[A-Z]{2}-\\d{2}|\\d{2}-\\d{2}-[A-Z]{2}";

    public LicensePlate {
        Objects.requireNonNull(value, "licensePlate");
        if (!value.matches(FORMAT)) {
            throw new InvalidLicensePlateException(value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
