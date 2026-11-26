package pt.rentporto.customer.domain.exception;

import java.util.UUID;
import pt.rentporto.web.error.BusinessException;

/**
 * There is no profile for the customer (404).
 */
public class CustomerNotFoundException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param id id of the customer
     */
    public CustomerNotFoundException(UUID id) {
        super("CUSTOMER_NOT_FOUND", Category.NOT_FOUND, "There is no profile for the customer " + id);
    }
}
