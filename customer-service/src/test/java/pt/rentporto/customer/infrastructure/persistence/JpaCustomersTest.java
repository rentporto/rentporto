package pt.rentporto.customer.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.customer.domain.exception.CustomerAlreadyRegisteredException;
import pt.rentporto.customer.domain.exception.TaxNumberAlreadyRegisteredException;
import pt.rentporto.customer.domain.model.Customer;
import pt.rentporto.customer.domain.model.CustomerDetails;
import pt.rentporto.customer.domain.model.DriverLicense;
import pt.rentporto.customer.domain.model.TaxNumber;
import pt.rentporto.customer.domain.repository.Customers;
import pt.rentporto.customer.testing.CustomerServiceTest;
import pt.rentporto.customer.testing.TaxNumbers;

/**
 * The unique constraints of the table become domain exceptions, so the application can tell them apart.
 */
@CustomerServiceTest
class JpaCustomersTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

    @Autowired
    private Customers customers;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void storingTheSameCustomerTwiceTellsTheSecondRequest() {
        UUID id = UUID.randomUUID();
        add(id, TaxNumbers.unique());

        assertThatThrownBy(() -> add(id, TaxNumbers.unique())).isInstanceOf(CustomerAlreadyRegisteredException.class);
    }

    @Test
    void storingATaxNumberOfAnotherCustomerIsAConflict() {
        String taxNumber = TaxNumbers.unique();
        add(UUID.randomUUID(), taxNumber);

        assertThatThrownBy(() -> add(UUID.randomUUID(), taxNumber))
                .isInstanceOf(TaxNumberAlreadyRegisteredException.class);
    }

    private void add(UUID id, String taxNumber) {
        DriverLicense license = new DriverLicense("P-1234567 8", "B", "PT", LocalDate.of(2010, 6, 1), TODAY);
        CustomerDetails details = new CustomerDetails(
                "Ana Ferreira", new TaxNumber(taxNumber), LocalDate.of(1990, 5, 10), "+351912345678", license);
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> customers.add(Customer.register(id, "ana@rentporto.pt", details, TODAY)));
    }
}
