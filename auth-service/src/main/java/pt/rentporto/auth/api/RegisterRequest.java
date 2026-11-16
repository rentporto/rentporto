package pt.rentporto.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Account a customer registers.
 *
 * @param email    email used to log in and to receive notifications
 * @param password password, 8 to 72 characters
 */
public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password) {
}
