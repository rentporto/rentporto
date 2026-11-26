package pt.rentporto.customer.domain.repository;

import java.util.Optional;
import java.util.UUID;
import pt.rentporto.customer.domain.exception.CustomerAlreadyRegisteredException;
import pt.rentporto.customer.domain.exception.TaxNumberAlreadyRegisteredException;
import pt.rentporto.customer.domain.model.Customer;
import pt.rentporto.customer.domain.model.TaxNumber;

/**
 * Stored customer profiles. Implemented in the infrastructure.
 */
public interface Customers {

    /**
     * Finds a profile.
     *
     * @param id id of the customer
     * @return the profile, if there is one
     */
    Optional<Customer> withId(UUID id);

    /**
     * Finds the profile with a tax number.
     *
     * @param taxNumber the tax number
     * @return the profile, if there is one
     */
    Optional<Customer> withTaxNumber(TaxNumber taxNumber);

    /**
     * Stores a new profile.
     *
     * @param customer the profile
     * @throws CustomerAlreadyRegisteredException  if a profile with the same id was stored first
     * @throws TaxNumberAlreadyRegisteredException if another profile has the same tax number
     */
    void add(Customer customer);

    /**
     * Stores the changes to a profile.
     *
     * @param customer the profile
     * @throws TaxNumberAlreadyRegisteredException if another profile has the same tax number
     */
    void update(Customer customer);
}
