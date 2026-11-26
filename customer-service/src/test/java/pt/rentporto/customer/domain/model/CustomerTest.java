package pt.rentporto.customer.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pt.rentporto.customer.domain.exception.DriverLicenseExpiredException;
import pt.rentporto.customer.domain.exception.InvalidDriverLicenseException;

class CustomerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);
    private static final UUID ID = UUID.fromString("3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b");

    @Test
    void registersAProfileWithALicenceValidToday() {
        Customer customer = Customer.register(ID, "ana@rentporto.pt", details(TODAY), TODAY);

        assertThat(customer.id()).isEqualTo(ID);
        assertThat(customer.email()).isEqualTo("ana@rentporto.pt");
        assertThat(customer.fullName()).isEqualTo("Ana Ferreira");
    }

    @Test
    void rejectsALicenceThatExpiredYesterday() {
        assertThatThrownBy(() -> Customer.register(ID, "ana@rentporto.pt", details(TODAY.minusDays(1)), TODAY))
                .isInstanceOf(DriverLicenseExpiredException.class)
                .extracting("code").isEqualTo("DRIVER_LICENSE_EXPIRED");
    }

    @Test
    void anUpdateAlsoNeedsAValidLicence() {
        Customer customer = Customer.register(ID, "ana@rentporto.pt", details(TODAY), TODAY);

        assertThatThrownBy(() -> customer.update("ana@rentporto.pt", details(TODAY), TODAY.plusDays(1)))
                .isInstanceOf(DriverLicenseExpiredException.class);
    }

    @Test
    void rejectsALicenceIssuedBeforeTheDateOfBirth() {
        DriverLicense license = new DriverLicense("P-1234567 8", "B", "PT", LocalDate.of(1989, 1, 1), TODAY);

        assertThatThrownBy(() -> new CustomerDetails(
                "Ana Ferreira", new TaxNumber("123456789"), LocalDate.of(1990, 5, 10), "+351912345678", license))
                .isInstanceOf(InvalidDriverLicenseException.class)
                .hasMessageContaining("date of birth");
    }

    private static CustomerDetails details(LocalDate licenceExpiry) {
        DriverLicense license = new DriverLicense("P-1234567 8", "B", "PT", LocalDate.of(2010, 6, 1), licenceExpiry);
        return new CustomerDetails(
                " Ana Ferreira ", new TaxNumber("123456789"), LocalDate.of(1990, 5, 10), "+351912345678", license);
    }
}
