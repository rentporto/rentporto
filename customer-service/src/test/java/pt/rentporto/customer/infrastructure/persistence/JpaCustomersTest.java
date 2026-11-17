package pt.rentporto.customer.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.customer.CustomerServiceTest;
import pt.rentporto.customer.TaxNumbers;
import pt.rentporto.customer.domain.Customer;
import pt.rentporto.customer.domain.CustomerAlreadyRegisteredException;
import pt.rentporto.customer.domain.CustomerDetails;
import pt.rentporto.customer.domain.Customers;
import pt.rentporto.customer.domain.DriverLicense;
import pt.rentporto.customer.domain.TaxNumber;
import pt.rentporto.customer.domain.TaxNumberAlreadyRegisteredException;

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
