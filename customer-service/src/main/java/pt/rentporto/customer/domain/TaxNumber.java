package pt.rentporto.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Portuguese tax number (NIF): nine digits, the last one a check digit over the first eight.
 *
 * @param value the nine digits
 */
@Embeddable
public record TaxNumber(@Column(name = "tax_number", nullable = false) String value) {

    public TaxNumber {
        Objects.requireNonNull(value, "taxNumber");
        if (!value.matches("\\d{9}") || checkDigit(value) != value.charAt(8) - '0') {
            throw new InvalidTaxNumberException(value);
        }
    }

    private static int checkDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += (digits.charAt(i) - '0') * (9 - i);
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    @Override
    public String toString() {
        return value;
    }
}
