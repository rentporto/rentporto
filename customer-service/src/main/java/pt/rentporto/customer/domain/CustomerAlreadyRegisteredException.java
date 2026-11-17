package pt.rentporto.customer.domain;

import java.util.UUID;

/**
 * Another request stored the profile of the same customer first. The application then updates that profile instead,
 * so this exception never reaches the client.
 */
public class CustomerAlreadyRegisteredException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param id id of the customer
     */
    public CustomerAlreadyRegisteredException(UUID id) {
        super("The profile of the customer " + id + " was stored by another request");
    }
}
