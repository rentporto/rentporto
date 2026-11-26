package pt.rentporto.fleet.domain.exception;

import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.web.error.BusinessException;

/**
 * There is no category with the code (404).
 */
public class CategoryNotFoundException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param code ACRISS code of the category
     */
    public CategoryNotFoundException(AcrissCode code) {
        super("CATEGORY_NOT_FOUND", Category.NOT_FOUND, "There is no category " + code);
    }
}
