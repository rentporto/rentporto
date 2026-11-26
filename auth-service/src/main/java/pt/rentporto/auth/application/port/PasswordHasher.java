package pt.rentporto.auth.application.port;

import pt.rentporto.auth.application.exception.PasswordTooLongException;

/**
 * Hashes passwords and checks them. Implemented in the infrastructure.
 */
public interface PasswordHasher {

    /**
     * Hashes a new password.
     *
     * @param password password in clear text
     * @return the hash to store
     * @throws PasswordTooLongException if the algorithm cannot use the whole password
     */
    String hash(String password);

    /**
     * Checks a password against a stored hash. Without a hash, because there is no account with the email, it takes
     * as long as a real check, so the time of the answer does not reveal whether the account exists.
     *
     * @param password password in clear text
     * @param hash     stored hash, or {@code null} if there is no account
     * @return {@code true} if the password matches the hash
     */
    boolean matches(String password, String hash);
}
