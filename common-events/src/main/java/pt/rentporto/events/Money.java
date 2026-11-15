package pt.rentporto.events;

/**
 * Amount of money in euro cents.
 *
 * @param amountCents amount in cents, e.g. {@code 4500} for €45.00; never negative
 * @param currency    ISO 4217 currency code; always {@code EUR}
 */
public record Money(long amountCents, String currency) {

    public static final String EUR = "EUR";

    public Money {
        if (!EUR.equals(currency)) {
            throw new IllegalArgumentException("currency must be EUR, was " + currency);
        }
        if (amountCents < 0) {
            throw new IllegalArgumentException("amountCents must be >= 0, was " + amountCents);
        }
    }

    /**
     * Creates an amount in euros.
     *
     * @param amountCents amount in cents, e.g. {@code 4500} for €45.00
     * @return the amount in EUR
     */
    public static Money ofCents(long amountCents) {
        return new Money(amountCents, EUR);
    }
}
