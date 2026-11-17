package pt.rentporto.auth.application;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.rentporto.auth.application.TokenSigner.IssuedToken;
import pt.rentporto.auth.domain.Account;
import pt.rentporto.auth.domain.Accounts;
import pt.rentporto.auth.domain.Email;

/**
 * Logs users in and publishes the key that validates their tokens.
 */
@Service
public class LoginService {

    private final Accounts accounts;
    private final PasswordHasher passwordHasher;
    private final TokenSigner tokenSigner;

    LoginService(Accounts accounts, PasswordHasher passwordHasher, TokenSigner tokenSigner) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.tokenSigner = tokenSigner;
    }

    /**
     * Checks the credentials and issues a token.
     *
     * @param email    email of the account
     * @param password password in clear text
     * @return the token
     * @throws InvalidCredentialsException if the email is unknown or the password is wrong
     */
    @Transactional(readOnly = true)
    public IssuedToken login(String email, String password) {
        Optional<Account> account = accounts.withEmail(new Email(email));
        boolean matches = passwordHasher.matches(password, account.map(Account::passwordHash).orElse(null));
        if (!matches) {
            throw new InvalidCredentialsException();
        }
        return tokenSigner.sign(account.orElseThrow());
    }

    /**
     * Public key that validates the tokens, published at {@code /auth/jwks}.
     *
     * @return the key as a JWK Set
     */
    public Map<String, Object> publicKeys() {
        return tokenSigner.publicKeys();
    }
}
