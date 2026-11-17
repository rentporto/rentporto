package pt.rentporto.web.security;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.testing.TestJwtDecoder;

@SpringBootApplication
@EnableMethodSecurity
@Import(TestJwtDecoder.class)
class SecurityTestApplication {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ResourceServerSecurity resourceServer) {
        return resourceServer.apply(http, ResourceServerSecurity.publicEndpoints(
                PathPatternRequestMatcher.withDefaults().matcher("/public"))).build();
    }

    @RestController
    static class Endpoints {

        @GetMapping("/public")
        String open() {
            return "open";
        }

        @GetMapping("/protected")
        String authenticated() {
            return "authenticated";
        }

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String admin() {
            return "admin";
        }
    }
}
