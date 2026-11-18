package pt.rentporto.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AcrissCodeTest {

    @ParameterizedTest
    @ValueSource(strings = {"MBMR", "ECMR", "CDMR", "IFAR", "PDAR", "LDAE", "XKND"})
    void acceptsALetterOfTheTableInEachPosition(String value) {
        assertThat(new AcrissCode(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACMR", "EAMR", "ECER", "ECMT", "ecmr", "ECM", "ECMRR", ""})
    void rejectsALetterOutsideTheTableOrTheWrongLength(String value) {
        assertThatThrownBy(() -> new AcrissCode(value))
                .isInstanceOf(InvalidCategoryCodeException.class)
                .extracting("code").isEqualTo("INVALID_CATEGORY_CODE");
    }
}
