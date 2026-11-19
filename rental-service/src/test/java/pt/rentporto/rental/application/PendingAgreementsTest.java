package pt.rentporto.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.rental.Reservations;
import pt.rentporto.rental.domain.ConfirmedReservation;
import pt.rentporto.rental.domain.DiscardReason;
import pt.rentporto.rental.domain.InconsistentEventException;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalStatus;

class PendingAgreementsTest {

    private final InMemoryAgreements agreements = new InMemoryAgreements();
    private final RecordedRentalEvents events = new RecordedRentalEvents();
    private final PendingAgreements pending =
            new PendingAgreements(agreements, events, mock(PlatformTransactionManager.class));
    private final ConfirmedReservation reservation = Reservations.confirmed();

    @Test
    void aConfirmedReservationGetsOneAgreementWaitingForThePickUp() {
        assertThat(pending.reservationConfirmed(reservation)).isTrue();
        RentalAgreement agreement = agreements.forReservation(reservation.reservationId()).orElseThrow();

        assertThat(pending.reservationConfirmed(reservation)).isFalse();

        assertThat(agreements.forReservation(reservation.reservationId())).containsSame(agreement);
        assertThat(agreement.status()).isEqualTo(RentalStatus.AWAITING_PICKUP);
    }

    @Test
    void aCancelledReservationDiscardsItsAgreementOnce() {
        pending.reservationConfirmed(reservation);

        assertThat(pending.reservationDiscarded(reservation.reservationId(), DiscardReason.CANCELLED)).isTrue();
        assertThat(pending.reservationDiscarded(reservation.reservationId(), DiscardReason.NO_SHOW)).isFalse();

        RentalAgreement agreement = agreements.forReservation(reservation.reservationId()).orElseThrow();
        assertThat(agreement.status()).isEqualTo(RentalStatus.DISCARDED);
        assertThat(agreement.discardReason()).isEqualTo(DiscardReason.CANCELLED);
    }

    @Test
    void discardingAnAgreementWithADepositHeldReleasesIt() {
        pending.reservationConfirmed(reservation);
        RentalAgreement agreement = agreements.forReservation(reservation.reservationId()).orElseThrow();
        agreement.recordDeposit(UUID.randomUUID());

        pending.reservationDiscarded(reservation.reservationId(), DiscardReason.NO_SHOW);
        pending.reservationDiscarded(reservation.reservationId(), DiscardReason.NO_SHOW);

        assertThat(events.settled).containsExactly(agreement.id() + "=0");
    }

    @Test
    void discardingAnAgreementWithoutADepositReleasesNothing() {
        pending.reservationConfirmed(reservation);

        pending.reservationDiscarded(reservation.reservationId(), DiscardReason.CANCELLED);

        assertThat(events.settled).isEmpty();
    }

    @Test
    void aReservationWithoutAnAgreementCannotBeDiscarded() {
        assertThatThrownBy(() -> pending.reservationDiscarded(UUID.randomUUID(), DiscardReason.NO_SHOW))
                .isInstanceOf(InconsistentEventException.class);
    }
}
