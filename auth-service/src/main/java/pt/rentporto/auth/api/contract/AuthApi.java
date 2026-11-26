package pt.rentporto.auth.api.contract;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.rentporto.auth.api.request.LoginRequest;
import pt.rentporto.auth.api.request.RegisterRequest;
import pt.rentporto.auth.api.response.TokenResponse;
import pt.rentporto.auth.api.response.UserResponse;

/**
 * Registration, login and the public key that validates the tokens.
 */
@Tag(name = "Authentication")
public interface AuthApi {

    /**
     * Creates a customer account. Public.
     *
     * @param request        email and password
     * @param idempotencyKey key that makes a repeated request return the same account
     * @return the account created, with the CUSTOMER role
     */
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    UserResponse register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey);

    /**
     * Authenticates a user and issues a JWT. Public.
     *
     * @param request email and password
     * @return the access token
     */
    @PostMapping("/auth/login")
    @SecurityRequirements
    TokenResponse login(@Valid @RequestBody LoginRequest request);

    /**
     * Public key used by the gateway and the services to validate the JWTs. Public.
     *
     * @return the JWK Set
     */
    @GetMapping("/auth/jwks")
    @SecurityRequirements
    Map<String, Object> jwks();
}
