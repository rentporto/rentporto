package pt.rentporto.auth.api;

import java.util.Set;
import java.util.UUID;

/**
 * User account.
 *
 * @param userId account identifier; for a customer, it is also the customer identifier
 * @param email  email used to log in
 * @param roles  roles of the account
 */
public record UserResponse(UUID userId, String email, Set<Role> roles) {
}
