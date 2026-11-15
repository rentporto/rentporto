package pt.rentporto.events.customer;

import java.util.Objects;
import java.util.UUID;

/**
 * A customer created their profile.
 *
 * <p>Published by the customer-service on {@code customer.registered}; consumed by the notification-service.
 *
 * @param customerId customer identifier, equal to the user id issued by the auth-service
 * @param fullName   customer's full name
 * @param email      address the notifications are sent to
 */
public record CustomerRegistered(UUID customerId, String fullName, String email) {

    public CustomerRegistered {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(email, "email");
    }
}
