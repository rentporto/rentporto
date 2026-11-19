package pt.rentporto.rental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Amount in euro, in cents. Every amount of RentPorto is in euro.
 *
 * @param cents amount in cents, e.g. {@code 4500} for €45.00; never negative
 */
@Embeddable
public record Money(@Column(name = "cents", nullable = false) long cents) {

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("cents must be >= 0, was " + cents);
        }
    }

    /**
     * Creates an amount.
     *
     * @param cents amount in cents
     * @return the amount
     */
    public static Money ofCents(long cents) {
        return new Money(cents);
    }
}
