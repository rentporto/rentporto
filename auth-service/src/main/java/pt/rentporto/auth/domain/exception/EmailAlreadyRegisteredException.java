package pt.rentporto.auth.domain.exception;

import pt.rentporto.auth.domain.model.Email;
import pt.rentporto.web.error.BusinessException;

/**
 * An account with this email already exists (409).
 */
public class EmailAlreadyRegisteredException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param email email of the existing account
     */
    public EmailAlreadyRegisteredException(Email email) {
        super("EMAIL_ALREADY_REGISTERED", Category.CONFLICT, "An account with the email " + email + " already exists");
    }
}
