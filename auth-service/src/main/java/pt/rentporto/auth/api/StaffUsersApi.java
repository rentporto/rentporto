package pt.rentporto.auth.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Staff accounts.
 */
@Tag(name = "Users")
public interface StaffUsersApi {

    /**
     * Creates an agent or manager account. Roles: ADMIN.
     *
     * @param request        email, initial password and roles
     * @param idempotencyKey key that makes a repeated request return the same account
     * @return the account created
     */
    @PostMapping("/auth/users")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse createStaffUser(
            @Valid @RequestBody CreateStaffUserRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey);
}
