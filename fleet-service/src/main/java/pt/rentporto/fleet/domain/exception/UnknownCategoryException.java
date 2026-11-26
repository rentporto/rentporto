package pt.rentporto.fleet.domain.exception;

import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.web.error.BusinessException;

/**
 * A vehicle refers to a category that does not exist (422).
 */
public class UnknownCategoryException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param code ACRISS code of the category
     */
    public UnknownCategoryException(AcrissCode code) {
        super("UNKNOWN_CATEGORY", Category.UNPROCESSABLE, "There is no category " + code);
    }
}
