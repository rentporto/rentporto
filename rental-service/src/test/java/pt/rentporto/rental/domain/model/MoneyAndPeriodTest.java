package pt.rentporto.rental.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MoneyAndPeriodTest {

    private static final Instant NINE = Instant.parse("2026-07-01T09:00:00Z");

    @Test
    void anAmountIsNeverNegative() {
        assertThat(Money.ofCents(0).cents()).isZero();
        assertThatThrownBy(() -> Money.ofCents(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theReturnIsAfterThePickUp() {
        assertThat(new RentalPeriod(NINE, NINE.plusSeconds(1)).returnAt()).isEqualTo(NINE.plusSeconds(1));
        assertThatThrownBy(() -> new RentalPeriod(NINE, NINE)).isInstanceOf(IllegalArgumentException.class);
    }
}
