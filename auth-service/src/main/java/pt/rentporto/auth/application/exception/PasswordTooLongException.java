package pt.rentporto.auth.application.exception;

import pt.rentporto.web.error.BusinessException;

/**
 * The password is longer than the hashing algorithm can use (422). With BCrypt, the limit is 72 bytes, which is only
 * reached with characters that take more than one byte, since the request already limits the password to 72
 * characters.
 */
public class PasswordTooLongException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param maxBytes maximum length of the password in UTF-8 bytes
     */
    public PasswordTooLongException(int maxBytes) {
        super("PASSWORD_TOO_LONG", Category.UNPROCESSABLE,
                "The password cannot be longer than " + maxBytes + " bytes in UTF-8");
    }
}
