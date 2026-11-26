package pt.rentporto.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pt.rentporto.auth.application.exception.PasswordTooLongException;
import pt.rentporto.auth.application.port.PasswordHasher;

/**
 * BCrypt, which only uses the first 72 bytes of a password.
 */
@Component
class BcryptPasswordHasher implements PasswordHasher {

    static final int MAX_BYTES = 72;

    private final PasswordEncoder encoder;
    private final String unknownAccountHash;

    BcryptPasswordHasher() {
        this(new BCryptPasswordEncoder());
    }

    BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.unknownAccountHash = encoder.encode("there is no account with this email");
    }

    @Override
    public String hash(String password) {
        if (tooLong(password)) {
            throw new PasswordTooLongException(MAX_BYTES);
        }
        return encoder.encode(password);
    }

    @Override
    public boolean matches(String password, String hash) {
        if (tooLong(password)) {
            return false;
        }
        if (hash == null) {
            encoder.matches(password, unknownAccountHash);
            return false;
        }
        return encoder.matches(password, hash);
    }

    private static boolean tooLong(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }
}
