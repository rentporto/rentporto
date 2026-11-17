package pt.rentporto.customer.api;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The account of the request, from its JWT: the {@code sub} is the id and the {@code email} claim the email.
 */
final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    static UUID id() {
        return UUID.fromString(token().getSubject());
    }

    static String email() {
        return token().getClaimAsString("email");
    }

    private static Jwt token() {
        return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();
    }
}
