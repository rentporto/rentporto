package pt.rentporto.fleet.domain;

import pt.rentporto.web.BusinessException;

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
