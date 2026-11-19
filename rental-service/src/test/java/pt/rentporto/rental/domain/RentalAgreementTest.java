package pt.rentporto.rental.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import pt.rentporto.rental.Reservations;

class RentalAgreementTest {

    private static final Instant PICKED_UP_AT = Instant.parse("2026-07-01T09:05:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 1);
    private static final AdditionalDriver RUI = new AdditionalDriver(
            "Rui Costa", "P-7654321 0", "B", "PT", LocalDate.of(2015, 3, 1), LocalDate.of(2030, 3, 1));

    private final ConfirmedReservation reservation = Reservations.confirmed();
    private final RentalAgreement agreement = RentalAgreement.awaitingPickup(reservation);

    @Test
    void anAgreementKeepsEverythingTheReservationAgreed() {
        assertThat(agreement.status()).isEqualTo(RentalStatus.AWAITING_PICKUP);
        assertThat(agreement.reservationId()).isEqualTo(reservation.reservationId());
        assertThat(agreement.customerId()).isEqualTo(reservation.customerId());
        assertThat(agreement.pickupStation()).isEqualTo("OPO");
        assertThat(agreement.returnStation()).isEqualTo("MAT");
        assertThat(agreement.categoryCode()).isEqualTo("ECMR");
        assertThat(agreement.period()).isEqualTo(reservation.period());
        assertThat(agreement.terms()).isEqualTo(reservation.terms());
        assertThat(agreement.total()).isEqualTo(reservation.total());
        assertThat(agreement.discardReason()).isNull();
    }

    @ParameterizedTest
    @EnumSource(DiscardReason.class)
    void anAgreementWaitingForThePickUpIsDiscardedOnceWithItsReason(DiscardReason reason) {
        assertThat(agreement.discard(reason)).isTrue();
        assertThat(agreement.status()).isEqualTo(RentalStatus.DISCARDED);
        assertThat(agreement.discardReason()).isEqualTo(reason);

        assertThat(agreement.discard(DiscardReason.CANCELLED)).isFalse();
        assertThat(agreement.discardReason()).isEqualTo(reason);
    }

    @Test
    void aPickUpMakesTheAgreementActiveWithTheVehicleAFullTankAndTheDrivers() {
        UUID deposit = UUID.randomUUID();
        agreement.recordDeposit(deposit);

        agreement.pickUp("AA-12-BB", 12000, List.of(RUI), "Scratch on the left door", PICKED_UP_AT, TODAY);

        assertThat(agreement.status()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(agreement.depositId()).isEqualTo(deposit);
        assertThat(agreement.licensePlate()).isEqualTo("AA-12-BB");
        assertThat(agreement.pickupOdometerKm()).isEqualTo(12000);
        assertThat(agreement.pickupFuelLevelEighths()).isEqualTo(8);
        assertThat(agreement.additionalDrivers()).containsExactly(RUI);
        assertThat(agreement.pickupNotes()).isEqualTo("Scratch on the left door");
        assertThat(agreement.pickedUpAt()).isEqualTo(PICKED_UP_AT);
    }

    @Test
    void thereIsNoPickUpWithoutADeposit() {
        assertThatThrownBy(() -> agreement.pickUp("AA-12-BB", 12000, List.of(), null, PICKED_UP_AT, TODAY))
                .isInstanceOf(IllegalStateException.class);
        assertThat(agreement.status()).isEqualTo(RentalStatus.AWAITING_PICKUP);
    }

    @Test
    void aVehicleIsPickedUpOnceAndAnActiveAgreementIsNotDiscarded() {
        agreement.recordDeposit(UUID.randomUUID());
        agreement.pickUp("AA-12-BB", 12000, List.of(), null, PICKED_UP_AT, TODAY);

        assertThatThrownBy(() -> agreement.pickUp("CC-34-DD", 500, List.of(), null, PICKED_UP_AT, TODAY))
                .isInstanceOf(InvalidRentalStatusException.class)
                .extracting("code").isEqualTo("INVALID_RENTAL_STATUS");
        assertThatThrownBy(() -> agreement.recordDeposit(UUID.randomUUID()))
                .isInstanceOf(InvalidRentalStatusException.class);
        assertThatThrownBy(() -> agreement.discard(DiscardReason.NO_SHOW))
                .isInstanceOf(InconsistentEventException.class);
        assertThat(agreement.licensePlate()).isEqualTo("AA-12-BB");
        assertThat(agreement.status()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void aDiscardedAgreementIsNotPickedUp() {
        agreement.discard(DiscardReason.CANCELLED);

        assertThatThrownBy(agreement::requireAwaitingPickup).isInstanceOf(InvalidRentalStatusException.class);
    }

    @ParameterizedTest
    @CsvSource({"2026-07-02, 2030-03-01", "2010-03-01, 2026-06-30"})
    void anAdditionalDriverNeedsALicenceIssuedAndNotExpiredOnTheDayOfThePickUp(LocalDate issue, LocalDate expiry) {
        AdditionalDriver driver = new AdditionalDriver("Rui Costa", "P-7654321 0", "B", "PT", issue, expiry);
        agreement.recordDeposit(UUID.randomUUID());

        assertThatThrownBy(() -> agreement.pickUp("AA-12-BB", 12000, List.of(driver), null, PICKED_UP_AT, TODAY))
                .isInstanceOf(DriverLicenseNotValidException.class)
                .extracting("code").isEqualTo("DRIVER_LICENSE_NOT_VALID");
        assertThat(agreement.status()).isEqualTo(RentalStatus.AWAITING_PICKUP);
    }

    @ParameterizedTest
    @CsvSource({"2026-07-01, 2030-03-01", "2010-03-01, 2026-07-01"})
    void aLicenceIssuedOrExpiringOnTheDayOfThePickUpIsValid(LocalDate issue, LocalDate expiry) {
        AdditionalDriver driver = new AdditionalDriver("Rui Costa", "P-7654321 0", "B", "PT", issue, expiry);

        driver.requireValidLicenseOn(TODAY);
    }
}
