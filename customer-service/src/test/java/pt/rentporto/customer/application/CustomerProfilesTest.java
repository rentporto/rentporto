package pt.rentporto.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.customer.domain.Customer;
import pt.rentporto.customer.domain.CustomerAlreadyRegisteredException;
import pt.rentporto.customer.domain.CustomerDetails;
import pt.rentporto.customer.domain.CustomerNotFoundException;
import pt.rentporto.customer.domain.Customers;
import pt.rentporto.customer.domain.DriverLicense;
import pt.rentporto.customer.domain.TaxNumber;
import pt.rentporto.customer.domain.TaxNumberAlreadyRegisteredException;

class CustomerProfilesTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneId.of("Europe/Lisbon"));
    private static final UUID ANA = UUID.fromString("3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b");
    private static final UUID RUI = UUID.fromString("9b8a7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");

    private final InMemoryCustomers customers = new InMemoryCustomers();
    private final List<Customer> published = new ArrayList<>();
    private final CustomerProfiles profiles =
            new CustomerProfiles(customers, published::add, CLOCK, mock(PlatformTransactionManager.class));

    @Test
    void theFirstSaveRegistersTheCustomerAndPublishesTheEvent() {
        Customer customer = profiles.save(ANA, "ana@rentporto.pt", details("Ana Ferreira", "123456789"));

        assertThat(customers.withId(ANA)).contains(customer);
        assertThat(published).containsExactly(customer);
    }

    @Test
    void theNextSavesUpdateWithoutPublishing() {
        profiles.save(ANA, "ana@rentporto.pt", details("Ana Ferreira", "123456789"));

        profiles.save(ANA, "ana@rentporto.pt", details("Ana Sofia Ferreira", "123456789"));

        assertThat(customers.withId(ANA).orElseThrow().fullName()).isEqualTo("Ana Sofia Ferreira");
        assertThat(published).hasSize(1);
    }

    @Test
    void aFirstSaveThatLosesTheRaceBecomesAnUpdate() {
        customers.registeredMeanwhile = Customer.register(
                ANA, "ana@rentporto.pt", details("Ana Ferreira", "123456789"), LocalDate.now(CLOCK));

        Customer customer = profiles.save(ANA, "ana@rentporto.pt", details("Ana Sofia Ferreira", "123456789"));

        assertThat(customer.fullName()).isEqualTo("Ana Sofia Ferreira");
        assertThat(published).isEmpty();
    }

    @Test
    void aTaxNumberBelongsToOneCustomer() {
        profiles.save(ANA, "ana@rentporto.pt", details("Ana Ferreira", "123456789"));

        assertThatThrownBy(() -> profiles.save(RUI, "rui@rentporto.pt", details("Rui Costa", "123456789")))
                .isInstanceOf(TaxNumberAlreadyRegisteredException.class);
        assertThat(customers.withId(RUI)).isEmpty();
    }

    @Test
    void readingAProfileThatDoesNotExistIsNotFound() {
        assertThatThrownBy(() -> profiles.profile(RUI)).isInstanceOf(CustomerNotFoundException.class);
    }

    private static CustomerDetails details(String fullName, String taxNumber) {
        DriverLicense license = new DriverLicense(
                "P-1234567 8", "B", "PT", LocalDate.of(2010, 6, 1), LocalDate.of(2035, 6, 1));
        return new CustomerDetails(
                fullName, new TaxNumber(taxNumber), LocalDate.of(1990, 5, 10), "+351912345678", license);
    }

    /**
     * Profiles in memory. {@code registeredMeanwhile} plays another request that stores the same customer just
     * before this one.
     */
    private static final class InMemoryCustomers implements Customers {

        private final Map<UUID, Customer> byId = new HashMap<>();
        private Customer registeredMeanwhile;

        @Override
        public Optional<Customer> withId(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Customer> withTaxNumber(TaxNumber taxNumber) {
            return byId.values().stream().filter(customer -> customer.taxNumber().equals(taxNumber)).findFirst();
        }

        @Override
        public void add(Customer customer) {
            if (registeredMeanwhile != null) {
                byId.put(registeredMeanwhile.id(), registeredMeanwhile);
                registeredMeanwhile = null;
                throw new CustomerAlreadyRegisteredException(customer.id());
            }
            byId.put(customer.id(), customer);
        }

        @Override
        public void update(Customer customer) {
            byId.put(customer.id(), customer);
        }
    }
}
