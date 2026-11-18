package pt.rentporto.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Objects;

/**
 * ACRISS code of a vehicle category: four letters for the class, the body type, the transmission and drive, and the
 * fuel and air conditioning. Each letter must be one of the ACRISS table for its position.
 *
 * @param value the four letters, e.g. {@code ECMR}
 */
@Embeddable
public record AcrissCode(@Column(name = "code", nullable = false) String value) {

    private static final List<String> LETTERS = List.of(
            "MNEHCDIJSRFGPULWOX",
            "BCDWVLSTFJXPQZEMRHYNGK",
            "MNCABD",
            "RNDQHIECLSABMFVZUX");

    public AcrissCode {
        Objects.requireNonNull(value, "code");
        if (value.length() != LETTERS.size()) {
            throw new InvalidCategoryCodeException(value);
        }
        for (int i = 0; i < LETTERS.size(); i++) {
            if (LETTERS.get(i).indexOf(value.charAt(i)) < 0) {
                throw new InvalidCategoryCodeException(value);
            }
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
