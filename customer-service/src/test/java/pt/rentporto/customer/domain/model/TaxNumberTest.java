package pt.rentporto.customer.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.rentporto.customer.domain.exception.InvalidTaxNumberException;

class TaxNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "100000002", "100000100"})
    void acceptsANumberWithTheRightCheckDigit(String value) {
        assertThat(new TaxNumber(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456780", "100000101", "12345678A", "12345678"})
    void rejectsAWrongCheckDigitOrFormat(String value) {
        assertThatThrownBy(() -> new TaxNumber(value))
                .isInstanceOf(InvalidTaxNumberException.class)
                .extracting("code").isEqualTo("INVALID_TAX_NUMBER");
    }
}
