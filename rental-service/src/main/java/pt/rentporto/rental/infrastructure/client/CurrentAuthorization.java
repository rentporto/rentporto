package pt.rentporto.rental.infrastructure.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The {@code Authorization} header of the request being served: the clients send it on, so the other services see
 * the agent at the counter (ADR-22).
 */
final class CurrentAuthorization {

    private CurrentAuthorization() {
    }

    static String header() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new IllegalStateException("There is no request to take the Authorization header from");
        }
        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            throw new IllegalStateException("The request has no Authorization header");
        }
        return authorization;
    }
}
