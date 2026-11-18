package pt.rentporto.fleet.domain;

import pt.rentporto.web.BusinessException;

/**
 * A category with the same code already exists (409).
 */
public class CategoryAlreadyExistsException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param code ACRISS code of the category
     */
    public CategoryAlreadyExistsException(AcrissCode code) {
        super("CATEGORY_ALREADY_EXISTS", Category.CONFLICT, "The category " + code + " already exists");
    }
}
