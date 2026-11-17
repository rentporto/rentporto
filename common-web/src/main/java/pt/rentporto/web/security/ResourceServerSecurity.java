package pt.rentporto.web.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Security every RentPorto service and the gateway share: stateless, without CSRF (the client sends the JWT in the
 * {@code Authorization} header), the public endpoints open and the others behind a JWT, with the roles of the
 * {@code roles} claim as authorities and the errors as ProblemDetail.
 */
public class ResourceServerSecurity {

    private final SecurityProblems problems;
    private final JwtAuthenticationConverter rolesAsAuthorities;

    /**
     * Creates the configurer.
     *
     * @param problems           answers 401 and 403 as ProblemDetail
     * @param rolesAsAuthorities turns the {@code roles} claim into authorities
     */
    public ResourceServerSecurity(SecurityProblems problems, JwtAuthenticationConverter rolesAsAuthorities) {
        this.problems = problems;
        this.rolesAsAuthorities = rolesAsAuthorities;
    }

    /**
     * Applies the shared security. A public endpoint ignores any token, so that a client that still sends an expired
     * one, for instance when it logs in again, is not answered with 401.
     *
     * @param http            the security of the service
     * @param publicEndpoints the endpoints open without a token
     * @return the same {@code http}, for the service to add what is its own
     */
    public HttpSecurity apply(HttpSecurity http, RequestMatcher publicEndpoints) {
        DefaultBearerTokenResolver bearerTokens = new DefaultBearerTokenResolver();
        BearerTokenResolver ignoringPublicEndpoints =
                request -> publicEndpoints.matches(request) ? null : bearerTokens.resolve(request);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(publicEndpoints).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server
                        .bearerTokenResolver(ignoringPublicEndpoints)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesAsAuthorities))
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems));
    }
}
