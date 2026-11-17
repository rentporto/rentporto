package pt.rentporto.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TokenConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(TokenConfiguration.class);

    @Test
    void doesNotStartWhenTheSecretOfTheKeyIsMissing() {
        runner.withPropertyValues("rentporto.auth.token.signing-key=${AUTH_SIGNING_KEY}")
                .run(context -> assertThat(context).getFailure()
                        .rootCause().hasMessageContaining("Could not resolve placeholder 'AUTH_SIGNING_KEY'"));
    }

    @Test
    void generatesAKeyWhenNoneIsConfigured() {
        runner.run(context -> assertThat(context).hasNotFailed().hasSingleBean(SigningKey.class));
    }
}
