package pt.rentporto.fleet.api;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The account of the request, from its JWT.
 */
final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    static String id() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
