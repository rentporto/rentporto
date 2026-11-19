package pt.rentporto.rental.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.rentporto.rental.Reservations;

/**
 * Charges worked out by hand with the terms of {@link Reservations#confirmed()}: €45.00 a day, a child seat at
 * €5.00 a day up to €25.00, an excess of €1000.00, €2.15 a litre and a refuelling fee of €25.00, for three days.
 */
class ReturnChargesTest {

    private static final ConfirmedReservation RESERVATION = Reservations.confirmed();
    private static final ContractTerms TERMS = RESERVATION.terms();
    private static final RentalPeriod PERIOD = RESERVATION.period();
    private static final Instant AGREED_RETURN = PERIOD.returnAt();
    private static final ReturnInspection FULL = inspection(8, "0");

    @ParameterizedTest(name = "{0} minutes late are {1} days")
    @CsvSource({"0, 0", "59, 0", "60, 1", "1439, 1", "1499, 1", "1500, 2", "2939, 2", "2940, 3"})
    void aDayIs24HoursWith59MinutesOfTolerance(long minutes, long days) {
        assertThat(ReturnCharges.days(Duration.ofMinutes(minutes))).isEqualTo(days);
        assertThat(ReturnCharges.days(Duration.ofMinutes(minutes).plusSeconds(59))).isEqualTo(days);
    }

    @Test
    void aReturnOnTimeOrUpTo59MinutesLateCostsNothing() {
        assertThat(ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN.minusSeconds(3600), FULL)).isEmpty();
        assertThat(ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN.plusSeconds(59 * 60 + 59), FULL)).isEmpty();
    }

    @Test
    void anHourLateIsOneDayWithTheExtrasWithinTheirCapPlusVat() {
        List<ExtraCharge> charges = ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN.plusSeconds(3600), FULL);

        assertThat(charges).extracting(ExtraCharge::type, ExtraCharge::description, ExtraCharge::amount)
                .containsExactly(tuple(ExtraCharge.Type.LATE_RETURN, "1 extra day", Money.ofCents(6150)));
    }

    @Test
    void theExtrasStopAtTheirCapCountingTheDaysAlreadyPaid() {
        assertThat(ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN.plus(Duration.ofHours(25)), FULL))
                .extracting(ExtraCharge::description, ExtraCharge::amount)
                .containsExactly(tuple("2 extra days", Money.ofCents(12300)));
        assertThat(ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN.plus(Duration.ofHours(73)), FULL))
                .extracting(ExtraCharge::description, ExtraCharge::amount)
                .containsExactly(tuple("4 extra days", Money.ofCents(23370)));
    }

    @Test
    void theVatIsRoundedOnceHalfUpOnTheSumOfTheLines() {
        ContractTerms terms = new ContractTerms(Money.ofCents(3850), TERMS.extras(), TERMS.coverage(), TERMS.excess(),
                TERMS.deposit(), TERMS.fuelPricePerLitre(), TERMS.refuellingFee());

        assertThat(ReturnCharges.of(terms, PERIOD, AGREED_RETURN.plusSeconds(3600), FULL))
                .extracting(ExtraCharge::amount)
                .containsExactly(Money.ofCents(5351));
    }

    @Test
    void theLitresToFillCostThePricePerLitreRoundedPlusTheRefuellingFeeOnce() {
        assertThat(ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN, inspection(3, "18.5")))
                .extracting(ExtraCharge::type, ExtraCharge::description, ExtraCharge::amount)
                .containsExactly(tuple(ExtraCharge.Type.FUEL, "18.5 litres to fill", Money.ofCents(6478)));
        assertThat(ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN, inspection(7, "0.01")))
                .extracting(ExtraCharge::amount)
                .containsExactly(Money.ofCents(2502));
    }

    @ParameterizedTest
    @CsvSource({"7, 0", "8, 5"})
    void litresThatDoNotMatchTheFuelLevelAreRefused(int eighths, String litres) {
        assertThatThrownBy(() -> ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN, inspection(eighths, litres)))
                .isInstanceOf(FuelMismatchException.class)
                .extracting("code").isEqualTo("FUEL_MISMATCH");
    }

    @Test
    void theDamagesTogetherStopAtTheExcess() {
        ReturnInspection damaged = new ReturnInspection(12450, 8, BigDecimal.ZERO, List.of(
                new ReturnDamage("FRONT_BUMPER", "10 cm scratch", Money.ofCents(60000)),
                new ReturnDamage("LEFT_DOOR", "Dent", Money.ofCents(50000)),
                new ReturnDamage("MIRROR", "Broken", Money.ofCents(10000))), null);

        assertThat(ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN, damaged))
                .extracting(ExtraCharge::type, ExtraCharge::description, ExtraCharge::amount)
                .containsExactly(
                        tuple(ExtraCharge.Type.DAMAGE, "FRONT_BUMPER: 10 cm scratch", Money.ofCents(60000)),
                        tuple(ExtraCharge.Type.DAMAGE, "LEFT_DOOR: Dent", Money.ofCents(40000)));
    }

    @Test
    void theChargesComeLateReturnFirstThenFuelThenTheDamages() {
        ReturnInspection everything = new ReturnInspection(12450, 4, new BigDecimal("25"),
                List.of(new ReturnDamage("MIRROR", "Broken", Money.ofCents(15000))), null);

        assertThat(ReturnCharges.of(TERMS, PERIOD, AGREED_RETURN.plusSeconds(7200), everything))
                .extracting(ExtraCharge::type)
                .containsExactly(ExtraCharge.Type.LATE_RETURN, ExtraCharge.Type.FUEL, ExtraCharge.Type.DAMAGE);
    }

    private static ReturnInspection inspection(int eighths, String litres) {
        return new ReturnInspection(12450, eighths, new BigDecimal(litres), List.of(), null);
    }
}
