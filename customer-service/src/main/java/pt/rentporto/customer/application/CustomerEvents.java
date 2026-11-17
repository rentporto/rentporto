package pt.rentporto.customer.application;

import pt.rentporto.customer.domain.Customer;

/**
 * Events the customer context publishes to the other services. Implemented in the infrastructure.
 */
public interface CustomerEvents {

    /**
     * A customer created their profile. Called in the transaction that stores it.
     *
     * @param customer the new profile
     */
    void registered(Customer customer);
}
