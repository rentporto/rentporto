package pt.rentporto.auth.api;

import java.util.Map;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.auth.application.AccountService;
import pt.rentporto.auth.application.LoginService;
import pt.rentporto.auth.application.TokenSigner.IssuedToken;

@RestController
class AuthController implements AuthApi {

    private final AccountService accounts;
    private final LoginService logins;

    AuthController(AccountService accounts, LoginService logins) {
        this.accounts = accounts;
        this.logins = logins;
    }

    @Override
    public UserResponse register(RegisterRequest request, String idempotencyKey) {
        return UserResponse.from(accounts.register(request.email(), request.password(), idempotencyKey));
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        IssuedToken token = logins.login(request.email(), request.password());
        return new TokenResponse(token.value(), "Bearer", Math.toIntExact(token.expiresIn().toSeconds()));
    }

    @Override
    public Map<String, Object> jwks() {
        return logins.publicKeys();
    }
}
