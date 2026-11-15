package pt.rentporto.events.fleet;

import java.util.Objects;

/**
 * A vehicle category was created or its requirements changed.
 *
 * <p>Published by the fleet-service on {@code fleet.vehicle-category-changed}; consumed by the reservation and
 * pricing modules of the booking-service.
 *
 * @param categoryCode        ACRISS code of the category, e.g. {@code ECMR}
 * @param name                category name, e.g. {@code Group B - Economy}
 * @param minimumAge          minimum driver age
 * @param minimumLicenseYears minimum years since the driving licence was issued
 */
public record VehicleCategoryChanged(String categoryCode, String name, int minimumAge, int minimumLicenseYears) {

    public VehicleCategoryChanged {
        Objects.requireNonNull(categoryCode, "categoryCode");
        Objects.requireNonNull(name, "name");
        if (minimumAge < 0) {
            throw new IllegalArgumentException("minimumAge must be >= 0, was " + minimumAge);
        }
        if (minimumLicenseYears < 0) {
            throw new IllegalArgumentException("minimumLicenseYears must be >= 0, was " + minimumLicenseYears);
        }
    }
}
