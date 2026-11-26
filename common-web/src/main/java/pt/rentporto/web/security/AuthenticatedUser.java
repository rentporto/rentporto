package pt.rentporto.web.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The account that made the current request, read from its JWT: the {@code sub} is the account, the {@code email}
 * claim its email and the {@code roles} claim its roles (ADR-08).
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    /**
     * Gives the subject of the token.
     *
     * @return the {@code sub} claim
     */
    public static String subject() {
        return authentication().getName();
    }

    /**
     * Gives the id of the account.
     *
     * @return the {@code sub} claim, which is the id of the account
     * @throws IllegalArgumentException if the subject is not a UUID
     */
    public static UUID id() {
        return UUID.fromString(subject());
    }

    /**
     * Gives the email of the account.
     *
     * @return the {@code email} claim, or {@code null} if the token has none
     */
    public static String email() {
        return ((JwtAuthenticationToken) authentication()).getToken().getClaimAsString("email");
    }

    /**
     * Tells whether the account has a role.
     *
     * @param role the role, without the {@code ROLE_} prefix, e.g. {@code AGENT}
     * @return {@code true} if the token grants it
     */
    public static boolean hasRole(String role) {
        return authentication().getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

    private static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
