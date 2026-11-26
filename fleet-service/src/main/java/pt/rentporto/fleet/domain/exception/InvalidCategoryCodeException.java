package pt.rentporto.fleet.domain.exception;

import pt.rentporto.web.error.BusinessException;

/**
 * The code of a category is not an ACRISS code (422).
 */
public class InvalidCategoryCodeException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param code the code as given
     */
    public InvalidCategoryCodeException(String code) {
        super("INVALID_CATEGORY_CODE", Category.UNPROCESSABLE, code + " is not an ACRISS code");
    }
}
