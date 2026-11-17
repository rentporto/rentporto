package pt.rentporto.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import pt.rentporto.auth.domain.Role;

/**
 * Staff account an administrator creates.
 *
 * @param email    email used to log in
 * @param password initial password, 8 to 72 characters
 * @param roles    roles of the account, e.g. {@code AGENT}
 */
public record CreateStaffUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty Set<@NotNull Role> roles) {
}
