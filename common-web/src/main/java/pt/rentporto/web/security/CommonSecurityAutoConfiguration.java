package pt.rentporto.web.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Shared security of a service that validates JWTs. Inactive until the service depends on Spring Security's resource
 * server; the JWK Set and the issuer come from {@code spring.security.oauth2.resourceserver.jwt}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({HttpSecurity.class, JwtAuthenticationConverter.class})
public class CommonSecurityAutoConfiguration {

    /** Claim of the JWT with the roles of the account, e.g. {@code ["CUSTOMER"]}. */
    public static final String ROLES_CLAIM = "roles";

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter rolesAsAuthorities() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityProblems securityProblems(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new SecurityProblems(resolver);
    }

    @Bean
    SecurityProblemsAdvice securityProblemsAdvice() {
        return new SecurityProblemsAdvice();
    }

    @Bean
    ResourceServerSecurity resourceServerSecurity(
            SecurityProblems securityProblems, JwtAuthenticationConverter rolesAsAuthorities) {
        return new ResourceServerSecurity(securityProblems, rolesAsAuthorities);
    }
}
