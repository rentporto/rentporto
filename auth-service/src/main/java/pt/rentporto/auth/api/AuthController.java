package pt.rentporto.auth.api;

import java.util.Map;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class AuthController implements AuthApi {

    @Override
    public UserResponse register(RegisterRequest request, String idempotencyKey) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public Map<String, Object> jwks() {
        throw new EndpointNotImplementedException();
    }
}
