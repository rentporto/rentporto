package pt.rentporto.fleet.domain;

import pt.rentporto.web.BusinessException;

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
