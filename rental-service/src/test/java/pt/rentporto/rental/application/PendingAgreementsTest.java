package pt.rentporto.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.rental.Reservations;
import pt.rentporto.rental.domain.ConfirmedReservation;
import pt.rentporto.rental.domain.DiscardReason;
import pt.rentporto.rental.domain.InconsistentEventException;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalStatus;

class PendingAgreementsTest {

    private final InMemoryAgreements agreements = new InMemoryAgreements();
    private final PendingAgreements pending =
            new PendingAgreements(agreements, mock(PlatformTransactionManager.class));
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
    void aReservationWithoutAnAgreementCannotBeDiscarded() {
        assertThatThrownBy(() -> pending.reservationDiscarded(UUID.randomUUID(), DiscardReason.NO_SHOW))
                .isInstanceOf(InconsistentEventException.class);
    }

    /**
     * Agreements in memory.
     */
    private static final class InMemoryAgreements implements RentalAgreements {

        private final Map<UUID, RentalAgreement> byReservation = new HashMap<>();

        @Override
        public Optional<RentalAgreement> forReservation(UUID reservationId) {
            return Optional.ofNullable(byReservation.get(reservationId));
        }

        @Override
        public void add(RentalAgreement agreement) {
            byReservation.put(agreement.reservationId(), agreement);
        }

        @Override
        public void update(RentalAgreement agreement) {
            byReservation.put(agreement.reservationId(), agreement);
        }
    }
}
