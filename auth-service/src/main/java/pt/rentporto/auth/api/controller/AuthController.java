package pt.rentporto.auth.api.controller;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.auth.api.contract.AuthApi;
import pt.rentporto.auth.api.request.LoginRequest;
import pt.rentporto.auth.api.request.RegisterRequest;
import pt.rentporto.auth.api.response.TokenResponse;
import pt.rentporto.auth.api.response.UserResponse;
import pt.rentporto.auth.application.port.TokenSigner.IssuedToken;
import pt.rentporto.auth.application.service.AccountService;
import pt.rentporto.auth.application.service.LoginService;

@RestController
class AuthController implements AuthApi {

    private final AccountService accounts;
    private final LoginService logins;

    AuthController(AccountService accounts, LoginService logins) {
        this.accounts = accounts;
        this.logins = logins;
    }

    @Override
    @PreAuthorize("permitAll()")
    public UserResponse register(RegisterRequest request, String idempotencyKey) {
        return UserResponse.from(accounts.register(request.email(), request.password(), idempotencyKey));
    }

    @Override
    @PreAuthorize("permitAll()")
    public TokenResponse login(LoginRequest request) {
        IssuedToken token = logins.login(request.email(), request.password());
        return new TokenResponse(token.value(), "Bearer", Math.toIntExact(token.expiresIn().toSeconds()));
    }

    @Override
    @PreAuthorize("permitAll()")
    public Map<String, Object> jwks() {
        return logins.publicKeys();
    }
}
