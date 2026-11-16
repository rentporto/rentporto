package pt.rentporto.customer.api;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Profile of a customer.
 *
 * @param customerId    customer identifier, equal to the user id issued by the auth-service
 * @param email         email of the account, from the JWT
 * @param fullName      full name as on the driving licence
 * @param taxNumber     Portuguese tax number (NIF)
 * @param birthDate     date of birth
 * @param phone         contact phone number
 * @param driverLicense driving licence
 */
public record CustomerResponse(
        UUID customerId,
        String email,
        String fullName,
        String taxNumber,
        LocalDate birthDate,
        String phone,
        DriverLicense driverLicense) {
}
