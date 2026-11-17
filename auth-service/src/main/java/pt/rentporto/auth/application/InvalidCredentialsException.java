package pt.rentporto.auth.application;

/**
 * The email is unknown or the password is wrong (401). The two cases are not told apart.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
