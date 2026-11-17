package pt.rentporto.auth.api;

import java.util.Set;
import java.util.UUID;
import pt.rentporto.auth.domain.Account;
import pt.rentporto.auth.domain.Role;

/**
 * User account.
 *
 * @param userId account identifier; for a customer, it is also the customer identifier
 * @param email  email used to log in
 * @param roles  roles of the account
 */
public record UserResponse(UUID userId, String email, Set<Role> roles) {

    static UserResponse from(Account account) {
        return new UserResponse(account.id(), account.email().value(), account.roles());
    }
}
