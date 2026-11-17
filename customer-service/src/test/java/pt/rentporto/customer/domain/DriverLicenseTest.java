package pt.rentporto.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DriverLicenseTest {

    private static final LocalDate ISSUED = LocalDate.of(2010, 6, 1);
    private static final LocalDate EXPIRES = LocalDate.of(2035, 6, 1);

    @Test
    void keepsTheNumberTheCategoryAndTheCountryInUpperCase() {
        DriverLicense license = new DriverLicense(" p-1234567 8 ", "be", "pt", ISSUED, EXPIRES);

        assertThat(license.number()).isEqualTo("P-1234567 8");
        assertThat(license.category()).isEqualTo("BE");
        assertThat(license.issuingCountry()).isEqualTo("PT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "B2", "CAR"})
    void rejectsACategoryThatDoesNotExist(String category) {
        assertThatThrownBy(() -> new DriverLicense("P-1234567 8", category, "PT", ISSUED, EXPIRES))
                .isInstanceOf(InvalidDriverLicenseException.class)
                .hasMessageContaining("category");
    }

    @ParameterizedTest
    @ValueSource(strings = {"P1", "-12345", "P_1234567", "123456789012345678901"})
    void rejectsAnImpossibleNumber(String number) {
        assertThatThrownBy(() -> new DriverLicense(number, "B", "PT", ISSUED, EXPIRES))
                .isInstanceOf(InvalidDriverLicenseException.class)
                .hasMessageContaining("number");
    }

    @Test
    void rejectsALicenceThatExpiresBeforeItWasIssued() {
        assertThatThrownBy(() -> new DriverLicense("P-1234567 8", "B", "PT", EXPIRES, ISSUED))
                .isInstanceOf(InvalidDriverLicenseException.class);
    }

    @Test
    void expiresTheDayAfterItsExpiryDate() {
        DriverLicense license = new DriverLicense("P-1234567 8", "B", "PT", ISSUED, EXPIRES);

        assertThat(license.expiredOn(EXPIRES)).isFalse();
        assertThat(license.expiredOn(EXPIRES.plusDays(1))).isTrue();
    }
}
