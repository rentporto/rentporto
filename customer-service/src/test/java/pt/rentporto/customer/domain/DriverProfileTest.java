package pt.rentporto.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Age and licence seniority on a date, with the boundary dates of the eligibility rules.
 */
class DriverProfileTest {

    private static final LocalDate BIRTH = LocalDate.of(2000, 5, 10);
    private static final LocalDate ISSUED = LocalDate.of(2024, 11, 18);
    private static final LocalDate EXPIRES = LocalDate.of(2034, 11, 18);

    @Test
    void theAgeCountsOnTheBirthday() {
        Customer customer = customer(BIRTH, ISSUED, EXPIRES);

        assertThat(customer.driverProfileOn(LocalDate.of(2025, 5, 9)).age()).isEqualTo(24);
        assertThat(customer.driverProfileOn(LocalDate.of(2025, 5, 10)).age()).isEqualTo(25);
    }

    @Test
    void someoneBornOn29FebruaryHasTheirBirthdayOn1MarchInOtherYears() {
        Customer customer = customer(LocalDate.of(2004, 2, 29), ISSUED, EXPIRES);

        assertThat(customer.driverProfileOn(LocalDate.of(2025, 2, 28)).age()).isEqualTo(20);
        assertThat(customer.driverProfileOn(LocalDate.of(2025, 3, 1)).age()).isEqualTo(21);
        assertThat(customer.driverProfileOn(LocalDate.of(2028, 2, 29)).age()).isEqualTo(24);
    }

    @Test
    void aLicenceIssuedExactlyTwoYearsAgoCountsTwoYears() {
        Customer customer = customer(BIRTH, ISSUED, EXPIRES);

        assertThat(customer.driverProfileOn(LocalDate.of(2026, 11, 17)).licenseYears()).isEqualTo(1);
        assertThat(customer.driverProfileOn(LocalDate.of(2026, 11, 18)).licenseYears()).isEqualTo(2);
    }

    @Test
    void theLicenceIsValidFromItsIssueDateToItsExpiryDate() {
        Customer customer = customer(BIRTH, ISSUED, EXPIRES);

        DriverProfile beforeIssue = customer.driverProfileOn(ISSUED.minusDays(1));
        assertThat(beforeIssue.licenseValid()).isFalse();
        assertThat(beforeIssue.licenseYears()).isZero();
        assertThat(customer.driverProfileOn(ISSUED.minusYears(2)).licenseYears()).isZero();
        assertThat(customer.driverProfileOn(ISSUED).licenseValid()).isTrue();
        assertThat(customer.driverProfileOn(EXPIRES).licenseValid()).isTrue();
        assertThat(customer.driverProfileOn(EXPIRES.plusDays(1)).licenseValid()).isFalse();
    }

    @Test
    void aDateBeforeTheBirthHasNoProfile() {
        Customer customer = customer(BIRTH, ISSUED, EXPIRES);

        assertThatThrownBy(() -> customer.driverProfileOn(BIRTH.minusDays(1)))
                .isInstanceOf(DateBeforeBirthException.class)
                .extracting("code").isEqualTo("DATE_BEFORE_BIRTH");
    }

    private static Customer customer(LocalDate birth, LocalDate issued, LocalDate expires) {
        DriverLicense license = new DriverLicense("P-1234567 8", "B", "PT", issued, expires);
        CustomerDetails details = new CustomerDetails(
                "Ana Ferreira", new TaxNumber("123456789"), birth, "+351912345678", license);
        return Customer.register(UUID.randomUUID(), "ana@rentporto.pt", details, issued);
    }
}
