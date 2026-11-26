package pt.rentporto.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pt.rentporto.auth.application.port.PasswordHasher;
import pt.rentporto.auth.domain.model.Account;
import pt.rentporto.auth.domain.model.Email;
import pt.rentporto.auth.domain.model.Role;
import pt.rentporto.auth.domain.repository.Accounts;
import pt.rentporto.auth.testing.AuthClient;
import pt.rentporto.auth.testing.AuthServiceTest;

/**
 * The administrator configured for the service exists after startup, once.
 */
@AuthServiceTest
class AdminBootstrapTest {

    @Autowired
    private AccountService service;

    @Autowired
    private Accounts accounts;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void createsTheAdministratorAtStartup() {
        Account admin = accounts.withEmail(new Email(AuthClient.ADMIN_EMAIL)).orElseThrow();

        assertThat(admin.roles()).containsExactly(Role.ADMIN);
        assertThat(passwordHasher.matches(AuthClient.ADMIN_PASSWORD, admin.passwordHash())).isTrue();
    }

    @Test
    void leavesTheAdministratorAsItIsOnTheNextStartup() {
        Account before = accounts.withEmail(new Email(AuthClient.ADMIN_EMAIL)).orElseThrow();

        new AdminBootstrap(service, AuthClient.ADMIN_EMAIL, "a-new-password").run(null);

        Account after = accounts.withEmail(new Email(AuthClient.ADMIN_EMAIL)).orElseThrow();
        assertThat(after.id()).isEqualTo(before.id());
        assertThat(passwordHasher.matches(AuthClient.ADMIN_PASSWORD, after.passwordHash())).isTrue();
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL, admin-password", "admin@rentporto.pt, NULL", "'', ''"}, nullValues = "NULL")
    void doesNothingWithoutAnEmailAndAPassword(String email, String password) {
        AccountService mockService = mock(AccountService.class);

        new AdminBootstrap(mockService, email, password).run(null);

        verifyNoInteractions(mockService);
    }

    @Test
    void doesNotStartWhenTheSecretOfThePasswordIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withBean(AccountService.class, () -> mock(AccountService.class))
                .withUserConfiguration(AdminBootstrap.class)
                .withPropertyValues(
                        "rentporto.auth.admin.email=admin@rentporto.pt",
                        "rentporto.auth.admin.password=${AUTH_ADMIN_PASSWORD}")
                .run(context -> assertThat(context).getFailure()
                        .rootCause().hasMessageContaining("Could not resolve placeholder 'AUTH_ADMIN_PASSWORD'"));
    }
}
