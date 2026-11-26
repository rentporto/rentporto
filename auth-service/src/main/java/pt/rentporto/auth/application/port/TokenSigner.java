package pt.rentporto.auth.application.port;

import java.time.Duration;
import java.util.Map;
import pt.rentporto.auth.domain.model.Account;

/**
 * Issues the signed tokens and publishes the key that validates them. Implemented in the infrastructure.
 */
public interface TokenSigner {

    /**
     * Issues a token for an account that has just logged in.
     *
     * @param account the account
     * @return the signed token
     */
    IssuedToken sign(Account account);

    /**
     * Public key that the gateway and the services use to validate the tokens.
     *
     * @return the key as a JWK Set
     */
    Map<String, Object> publicKeys();

    /**
     * Token issued at login.
     *
     * @param value     the signed token
     * @param expiresIn how long it is valid
     */
    record IssuedToken(String value, Duration expiresIn) {
    }
}
