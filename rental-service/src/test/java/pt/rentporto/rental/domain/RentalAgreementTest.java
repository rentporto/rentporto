package pt.rentporto.rental.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pt.rentporto.rental.Reservations;

class RentalAgreementTest {

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
}
