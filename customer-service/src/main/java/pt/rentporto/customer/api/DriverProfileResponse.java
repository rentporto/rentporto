package pt.rentporto.customer.api;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Age and licence seniority of a customer on a given date, used to check eligibility.
 *
 * @param customerId   customer identifier
 * @param on           date the profile refers to, usually the pick-up date
 * @param age          age in full years on that date
 * @param licenseYears full years since the licence was issued, on that date
 * @param licenseValid whether the licence has not expired on that date
 */
public record DriverProfileResponse(UUID customerId, LocalDate on, int age, int licenseYears, boolean licenseValid) {
}
