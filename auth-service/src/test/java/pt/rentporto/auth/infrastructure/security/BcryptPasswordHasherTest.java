package pt.rentporto.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.rentporto.auth.application.exception.PasswordTooLongException;

class BcryptPasswordHasherTest {

    private static final String FORTY_CHARACTERS_OF_TWO_BYTES = "ç".repeat(40);

    private final PasswordEncoder encoder = spy(new BCryptPasswordEncoder(4));
    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher(encoder);

    @Test
    void checksAPasswordAgainstItsBcryptHash() {
        String hash = hasher.hash("secret-password");

        assertThat(hash).startsWith("$2a$");
        assertThat(hasher.matches("secret-password", hash)).isTrue();
        assertThat(hasher.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void stillChecksAPasswordWhenThereIsNoAccount() {
        assertThat(hasher.matches("secret-password", null)).isFalse();

        verify(encoder).matches(eq("secret-password"), any());
    }

    @Test
    void rejectsAPasswordLongerThanTheBytesBcryptUses() {
        assertThatThrownBy(() -> hasher.hash(FORTY_CHARACTERS_OF_TWO_BYTES))
                .isInstanceOf(PasswordTooLongException.class);
        assertThat(hasher.matches(FORTY_CHARACTERS_OF_TWO_BYTES, hasher.hash("secret-password"))).isFalse();
    }

    @Test
    void doesNotAcceptTheRightPasswordWithMoreCharactersThatBcryptWouldCutOff() {
        String seventyTwoBytes = "a".repeat(72);

        assertThat(hasher.matches(seventyTwoBytes + "x", hasher.hash(seventyTwoBytes))).isFalse();
    }
}
