package pt.rentporto.fleet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.rentporto.fleet.domain.exception.InvalidLicensePlateException;

class LicensePlateTest {

    @ParameterizedTest
    @ValueSource(strings = {"AA-12-BB", "45-RT-12", "12-34-XZ"})
    void acceptsTheThreeFormatsSince1992(String value) {
        assertThat(new LicensePlate(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AA-12-34", "aa-12-bb", "AA12BB", "AA-12-BB ", "A-12-BB", ""})
    void rejectsAnythingElse(String value) {
        assertThatThrownBy(() -> new LicensePlate(value))
                .isInstanceOf(InvalidLicensePlateException.class)
                .extracting("code").isEqualTo("INVALID_LICENSE_PLATE");
    }
}
