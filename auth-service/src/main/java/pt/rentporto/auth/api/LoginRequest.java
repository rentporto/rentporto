package pt.rentporto.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credentials of a user logging in.
 *
 * @param email    email of the account
 * @param password password of the account
 */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
}
