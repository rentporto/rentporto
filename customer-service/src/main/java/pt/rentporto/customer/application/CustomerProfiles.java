package pt.rentporto.customer.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.customer.domain.Customer;
import pt.rentporto.customer.domain.CustomerAlreadyRegisteredException;
import pt.rentporto.customer.domain.CustomerDetails;
import pt.rentporto.customer.domain.CustomerNotFoundException;
import pt.rentporto.customer.domain.Customers;
import pt.rentporto.customer.domain.DriverLicenseExpiredException;
import pt.rentporto.customer.domain.TaxNumberAlreadyRegisteredException;

/**
 * Saves and reads customer profiles. The first save creates the profile and publishes {@code CustomerRegistered};
 * the next ones update it without publishing anything.
 */
@Service
public class CustomerProfiles {

    private final Customers customers;
    private final CustomerEvents events;
    private final Clock clock;
    private final TransactionTemplate transaction;

    CustomerProfiles(
            Customers customers, CustomerEvents events, Clock clock, PlatformTransactionManager transactionManager) {
        this.customers = customers;
        this.events = events;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Creates or updates the profile of a customer. When two first saves of the same customer arrive together, the
     * one that loses becomes an update, so the event is published once.
     *
     * @param id      id of the customer, the {@code sub} of the JWT
     * @param email   email of the account, from the JWT
     * @param details what the customer filled in
     * @return the stored profile
     * @throws DriverLicenseExpiredException       if the licence has expired
     * @throws TaxNumberAlreadyRegisteredException if another customer has the tax number
     */
    public Customer save(UUID id, String email, CustomerDetails details) {
        try {
            return transaction.execute(status -> saveOnce(id, email, details));
        } catch (CustomerAlreadyRegisteredException e) {
            return transaction.execute(status -> saveOnce(id, email, details));
        }
    }

    /**
     * Reads the profile of a customer.
     *
     * @param id id of the customer
     * @return the profile
     * @throws CustomerNotFoundException if the customer has no profile
     */
    public Customer profile(UUID id) {
        return transaction.execute(status -> customers.withId(id).orElseThrow(() -> new CustomerNotFoundException(id)));
    }

    private Customer saveOnce(UUID id, String email, CustomerDetails details) {
        LocalDate today = LocalDate.now(clock);
        customers.withTaxNumber(details.taxNumber())
                .filter(other -> !other.id().equals(id))
                .ifPresent(other -> {
                    throw new TaxNumberAlreadyRegisteredException(details.taxNumber());
                });
        Optional<Customer> existing = customers.withId(id);
        if (existing.isPresent()) {
            Customer customer = existing.get();
            customer.update(email, details, today);
            customers.update(customer);
            return customer;
        }
        Customer customer = Customer.register(id, email, details, today);
        customers.add(customer);
        events.registered(customer);
        return customer;
    }
}
