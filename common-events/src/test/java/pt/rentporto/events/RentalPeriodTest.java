package pt.rentporto.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RentalPeriodTest {

    private static final OffsetDateTime PICKUP_AT = OffsetDateTime.parse("2026-07-01T10:00:00+01:00");

    @Test
    void normalisesToUtcKeepingTheInstant() {
        RentalPeriod period = new RentalPeriod(PICKUP_AT, PICKUP_AT.plusDays(3));

        assertThat(period.pickupAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(period.returnAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(period.pickupAt().toInstant()).isEqualTo(PICKUP_AT.toInstant());
    }

    @Test
    void rejectsReturnAtPickup() {
        assertThatThrownBy(() -> new RentalPeriod(PICKUP_AT, PICKUP_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("returnAt");
    }

    @Test
    void rejectsReturnBeforePickup() {
        assertThatThrownBy(() -> new RentalPeriod(PICKUP_AT, PICKUP_AT.minusHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("returnAt");
    }
}
