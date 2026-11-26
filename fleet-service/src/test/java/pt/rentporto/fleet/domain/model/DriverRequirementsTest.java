package pt.rentporto.fleet.domain.model;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.rentporto.fleet.domain.exception.InvalidDriverRequirementsException;

class DriverRequirementsTest {

    @ParameterizedTest
    @CsvSource({"21, 2", "25, 2", "23, 5"})
    void acceptsTheMinimumOfTheCompanyOrMore(int minimumAge, int minimumLicenseYears) {
        assertThatNoException().isThrownBy(() -> new DriverRequirements(minimumAge, minimumLicenseYears));
    }

    @ParameterizedTest
    @CsvSource({"20, 2", "21, 1", "18, 0"})
    void rejectsLessThan21YearsOfAgeOr2OfLicence(int minimumAge, int minimumLicenseYears) {
        assertThatThrownBy(() -> new DriverRequirements(minimumAge, minimumLicenseYears))
                .isInstanceOf(InvalidDriverRequirementsException.class)
                .extracting("code").isEqualTo("INVALID_DRIVER_REQUIREMENTS");
    }
}
