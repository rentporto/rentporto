package pt.rentporto.auth.api;

import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class StaffUsersController implements StaffUsersApi {

    @Override
    public UserResponse createStaffUser(CreateStaffUserRequest request, String idempotencyKey) {
        throw new EndpointNotImplementedException();
    }
}
