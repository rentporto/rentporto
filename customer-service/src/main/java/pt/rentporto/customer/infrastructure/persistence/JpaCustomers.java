package pt.rentporto.customer.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import pt.rentporto.customer.domain.Customer;
import pt.rentporto.customer.domain.CustomerAlreadyRegisteredException;
import pt.rentporto.customer.domain.Customers;
import pt.rentporto.customer.domain.TaxNumber;
import pt.rentporto.customer.domain.TaxNumberAlreadyRegisteredException;

/**
 * Customer profiles in PostgreSQL. Each change is flushed at once, so that a broken unique constraint fails here, as a
 * domain exception, when two requests store the same customer or the same tax number at the same time.
 */
@Repository
class JpaCustomers implements Customers {

    static final String PRIMARY_KEY = "customers_pkey";
    static final String UNIQUE_TAX_NUMBER = "customers_tax_number_unique";

    private final SpringDataCustomers jpa;
    private final EntityManager entityManager;

    JpaCustomers(SpringDataCustomers jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Customer> withId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Customer> withTaxNumber(TaxNumber taxNumber) {
        return jpa.findByTaxNumber(taxNumber);
    }

    @Override
    public void add(Customer customer) {
        flush(customer, () -> entityManager.persist(customer));
    }

    @Override
    public void update(Customer customer) {
        flush(customer, () -> { });
    }

    private void flush(Customer customer, Runnable change) {
        try {
            change.run();
            entityManager.flush();
        } catch (PersistenceException e) {
            String constraint = constraintName(e);
            if (UNIQUE_TAX_NUMBER.equals(constraint)) {
                throw new TaxNumberAlreadyRegisteredException(customer.taxNumber());
            }
            if (PRIMARY_KEY.equals(constraint)) {
                throw new CustomerAlreadyRegisteredException(customer.id());
            }
            throw e;
        }
    }

    private static String constraintName(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
