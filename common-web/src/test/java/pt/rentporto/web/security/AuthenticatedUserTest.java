package pt.rentporto.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticatedUserTest {

    @AfterEach
    void forgetTheRequest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void theAccountComesFromTheTokenOfTheRequest() {
        UUID id = UUID.randomUUID();
        authenticate(id.toString(), "ana@rentporto.pt", "ROLE_CUSTOMER");

        assertThat(AuthenticatedUser.subject()).isEqualTo(id.toString());
        assertThat(AuthenticatedUser.id()).isEqualTo(id);
        assertThat(AuthenticatedUser.email()).isEqualTo("ana@rentporto.pt");
    }

    @Test
    void theRolesComeFromTheAuthoritiesOfTheToken() {
        authenticate(UUID.randomUUID().toString(), "agente@rentporto.pt", "ROLE_AGENT");

        assertThat(AuthenticatedUser.hasRole("AGENT")).isTrue();
        assertThat(AuthenticatedUser.hasRole("ADMIN")).isFalse();
        assertThat(AuthenticatedUser.hasRole("ROLE_AGENT")).isFalse();
    }

    @Test
    void aSubjectThatIsNotAnIdIsStillTheSubject() {
        authenticate("gestor", null, "ROLE_FLEET_MANAGER");

        assertThat(AuthenticatedUser.subject()).isEqualTo("gestor");
        assertThat(AuthenticatedUser.email()).isNull();
        assertThatThrownBy(AuthenticatedUser::id).isInstanceOf(IllegalArgumentException.class);
    }

    private static void authenticate(String subject, String email, String role) {
        Jwt.Builder token = Jwt.withTokenValue("token").header("alg", "none").subject(subject);
        if (email != null) {
            token.claim("email", email);
        }
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(token.build(), List.of(new SimpleGrantedAuthority(role))));
    }
}
