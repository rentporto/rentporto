package pt.rentporto.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void createsAmountInEuros() {
        assertThat(Money.ofCents(4500)).isEqualTo(new Money(4500, "EUR"));
    }

    @Test
    void rejectsCurrencyOtherThanEuro() {
        assertThatThrownBy(() -> new Money(4500, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> Money.ofCents(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amountCents");
    }
}
