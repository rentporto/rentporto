package pt.rentporto.auth.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.auth.application.AccountService;

@RestController
class StaffUsersController implements StaffUsersApi {

    private final AccountService accounts;

    StaffUsersController(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createStaffUser(CreateStaffUserRequest request, String idempotencyKey) {
        return UserResponse.from(
                accounts.createStaffUser(request.email(), request.password(), request.roles(), idempotencyKey));
    }
}
