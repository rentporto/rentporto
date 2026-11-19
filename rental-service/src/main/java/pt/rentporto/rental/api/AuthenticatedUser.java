package pt.rentporto.rental.api;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The account of the request, from its JWT: the {@code sub} is the id of the customer.
 */
final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    static UUID id() {
        return UUID.fromString(authentication().getName());
    }

    static boolean isAgent() {
        return authentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_AGENT".equals(authority.getAuthority()));
    }

    private static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
