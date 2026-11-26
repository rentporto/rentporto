package pt.rentporto.customer.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import pt.rentporto.customer.domain.exception.InvalidDriverLicenseException;

/**
 * Driving licence: the number and the category in upper case, the issuing country, and the issue and expiry dates.
 * Only the categories of the European driving licence are accepted.
 *
 * @param number         licence number, 5 to 20 letters, digits, spaces or hyphens
 * @param category       category, e.g. {@code B}
 * @param issuingCountry ISO 3166-1 alpha-2 code of the issuing country
 * @param issueDate      date the licence was issued
 * @param expiryDate     date the licence expires, not before the issue date
 */
@Embeddable
public record DriverLicense(
        @Column(name = "license_number", nullable = false) String number,
        @Column(name = "license_category", nullable = false) String category,
        @Column(name = "license_country", nullable = false) String issuingCountry,
        @Column(name = "license_issue_date", nullable = false) LocalDate issueDate,
        @Column(name = "license_expiry_date", nullable = false) LocalDate expiryDate) {

    private static final Set<String> CATEGORIES = Set.of(
            "AM", "A1", "A2", "A", "B1", "B", "BE", "C1", "C1E", "C", "CE", "D1", "D1E", "D", "DE");
    private static final Pattern NUMBER = Pattern.compile("[A-Z0-9][A-Z0-9 -]{3,18}[A-Z0-9]");

    public DriverLicense {
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(issuingCountry, "issuingCountry");
        Objects.requireNonNull(issueDate, "issueDate");
        Objects.requireNonNull(expiryDate, "expiryDate");
        number = number.strip().toUpperCase(Locale.ROOT);
        category = category.strip().toUpperCase(Locale.ROOT);
        issuingCountry = issuingCountry.strip().toUpperCase(Locale.ROOT);
        if (!NUMBER.matcher(number).matches()) {
            throw new InvalidDriverLicenseException("the number " + number + " is not a driving licence number");
        }
        if (!CATEGORIES.contains(category)) {
            throw new InvalidDriverLicenseException("the category " + category + " does not exist");
        }
        if (expiryDate.isBefore(issueDate)) {
            throw new InvalidDriverLicenseException("it expires before it was issued");
        }
    }

    /**
     * Whether the licence has expired on a date; it can still be used on its expiry date.
     *
     * @param date the date
     * @return {@code true} if the expiry date is before that date
     */
    public boolean expiredOn(LocalDate date) {
        return expiryDate.isBefore(date);
    }

    /**
     * Whether the licence can be used on a date.
     *
     * @param date the date
     * @return {@code true} if the licence was already issued and had not expired on that date
     */
    public boolean validOn(LocalDate date) {
        return !issueDate.isAfter(date) && !expiredOn(date);
    }

    /**
     * Full years since the licence was issued.
     *
     * @param date the date
     * @return the full years on that date, 0 if the licence was not issued yet
     */
    public int yearsOn(LocalDate date) {
        return date.isBefore(issueDate) ? 0 : Period.between(issueDate, date).getYears();
    }
}
