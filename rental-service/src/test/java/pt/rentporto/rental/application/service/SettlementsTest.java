package pt.rentporto.rental.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.rental.domain.exception.InconsistentEventException;
import pt.rentporto.rental.domain.model.Money;
import pt.rentporto.rental.domain.model.RentalAgreement;
import pt.rentporto.rental.domain.model.ReturnInspection;
import pt.rentporto.rental.domain.model.Settlement;
import pt.rentporto.rental.testing.Reservations;

class SettlementsTest {

    private static final Settlement SETTLED = new Settlement(Money.ofCents(0), Money.ofCents(60000), Money.ofCents(0));

    private final InMemoryAgreements agreements = new InMemoryAgreements();
    private final RecordedRentalEvents events = new RecordedRentalEvents();
    private final Settlements settlements =
            new Settlements(agreements, events, mock(PlatformTransactionManager.class));

    @Test
    void aSettledReturnClosesTheAgreementAndPublishesItOnce() {
        RentalAgreement agreement = returned();

        assertThat(settlements.depositSettled(agreement.id(), agreement.depositId(), SETTLED)).isTrue();
        assertThat(settlements.depositSettled(agreement.id(), agreement.depositId(), SETTLED)).isFalse();

        assertThat(events.closed).containsExactly(agreement);
    }

    @Test
    void aSettlementOfARentalThatDoesNotExistIsInconsistent() {
        assertThatThrownBy(() -> settlements.depositSettled(UUID.randomUUID(), UUID.randomUUID(), SETTLED))
                .isInstanceOf(InconsistentEventException.class);
        assertThat(events.closed).isEmpty();
    }

    private RentalAgreement returned() {
        RentalAgreement agreement = RentalAgreement.awaitingPickup(Reservations.confirmed());
        agreement.recordDeposit(UUID.randomUUID());
        agreement.pickUp("AA-12-BB", 12000, List.of(), null, Instant.parse("2026-07-01T09:00:00Z"),
                LocalDate.of(2026, 7, 1));
        agreement.returnAt("MAT", new ReturnInspection(12450, 8, BigDecimal.ZERO, List.of(), null),
                Instant.parse("2026-07-04T09:00:00Z"));
        agreements.add(agreement);
        return agreement;
    }
}
