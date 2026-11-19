package pt.rentporto.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Money;
import pt.rentporto.events.RentalPeriod;
import pt.rentporto.events.rental.ExtraCharge;
import pt.rentporto.events.rental.RentalClosed;
import pt.rentporto.events.rental.RentalStarted;
import pt.rentporto.events.reservation.ContractTerms;
import pt.rentporto.events.reservation.ReservationCancelled;
import pt.rentporto.events.reservation.ReservationConfirmed;
import pt.rentporto.events.reservation.ReservationNoShow;
import pt.rentporto.notification.application.Notice;
import pt.rentporto.notification.domain.PickupReminder;

class EmailTemplatesTest {

    private static final UUID CUSTOMER = UUID.fromString("3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b");
    private static final UUID RESERVATION = UUID.fromString("9b8a7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");

    private final EmailTemplates templates = new EmailTemplates(Clock.system(ZoneId.of("Europe/Lisbon")));

    @Test
    void theConfirmationShowsTheStationsTheDatesInLisbonWhatWasPaidAndTheDeposit() {
        ReservationConfirmed confirmed = new ReservationConfirmed(RESERVATION, CUSTOMER, "OPO", "MAT", "ECMR",
                new RentalPeriod(
                        OffsetDateTime.parse("2026-07-01T09:00:00Z"), OffsetDateTime.parse("2026-07-04T09:00:00Z")),
                new ContractTerms(Money.ofCents(4500), List.of(), "MEDIUM", Money.ofCents(100000),
                        Money.ofCents(60000), Money.ofCents(215), Money.ofCents(2500)),
                Money.ofCents(19680));

        Notice notice = templates.confirmed(envelope(confirmed));

        assertThat(notice.subject()).isEqualTo("Your reservation is confirmed");
        assertThat(notice.customerId()).isEqualTo(CUSTOMER);
        assertThat(notice.type()).isEqualTo("ReservationConfirmed");
        assertThat(notice.body())
                .contains("Pick-up: OPO, 1 Jul 2026, 10:00")
                .contains("Return: MAT, 4 Jul 2026, 10:00")
                .contains("Category: ECMR")
                .contains("Paid: €196.80")
                .contains("Security deposit held at pick-up: €600.00")
                .endsWith("RentPorto");
    }

    @Test
    void theCancellationShowsTheFeeAndTheRefundAndTheNoShowWhatWasKept() {
        assertThat(templates.cancelled(envelope(new ReservationCancelled(
                        RESERVATION, CUSTOMER, Money.ofCents(4500), Money.ofCents(15180)))).body())
                .contains("Cancellation fee: €45.00")
                .contains("Refund: €151.80");
        assertThat(templates.noShow(envelope(new ReservationNoShow(RESERVATION, CUSTOMER, Money.ofCents(4500)))))
                .satisfies(notice -> {
                    assertThat(notice.subject()).isEqualTo("You did not pick up your vehicle");
                    assertThat(notice.body()).contains("Amount kept: €45.00");
                });
    }

    @Test
    void thePickUpShowsTheVehicle() {
        assertThat(templates.started(envelope(new RentalStarted(UUID.randomUUID(), RESERVATION, CUSTOMER, "AA-12-BB",
                OffsetDateTime.parse("2026-07-01T09:05:00Z")))).body())
                .contains("You picked up the vehicle AA-12-BB on 1 Jul 2026, 10:05.");
    }

    @Test
    void theReminderShowsTheStationAndTheTimeOfThePickUpInLisbon() {
        PickupReminder reminder =
                PickupReminder.pending(RESERVATION, CUSTOMER, "OPO", Instant.parse("2026-07-01T09:00:00Z"));

        assertThat(templates.pickupReminder(reminder)).satisfies(notice -> {
            assertThat(notice.eventId()).isEqualTo(reminder.noticeId());
            assertThat(notice.customerId()).isEqualTo(CUSTOMER);
            assertThat(notice.type()).isEqualTo("PickupReminder");
            assertThat(notice.subject()).isEqualTo("Your pick-up is coming up");
            assertThat(notice.body())
                    .contains("reservation " + RESERVATION)
                    .contains("Pick-up: OPO, 1 Jul 2026, 10:00")
                    .endsWith("RentPorto");
        });
    }

    @Test
    void theReceiptListsTheChargesTheFinalTotalAndWhatIsStillOwed() {
        RentalClosed closed = new RentalClosed(UUID.randomUUID(), CUSTOMER, List.of(
                new ExtraCharge(ExtraCharge.Type.LATE_RETURN, "1 extra day", Money.ofCents(6150)),
                new ExtraCharge(ExtraCharge.Type.FUEL, "22.4 litres to fill", Money.ofCents(7316))),
                Money.ofCents(33146), Money.ofCents(2500));

        assertThat(templates.closed(envelope(closed))).satisfies(notice -> {
            assertThat(notice.subject()).isEqualTo("Your receipt");
            assertThat(notice.body())
                    .contains("- 1 extra day: €61.50\n- 22.4 litres to fill: €73.16")
                    .contains("Final total: €331.46")
                    .contains("Still owed: €25.00");
        });
    }

    @Test
    void aReceiptWithoutChargesSaysSoAndOwesNothing() {
        RentalClosed closed = new RentalClosed(UUID.randomUUID(), CUSTOMER, List.of(), Money.ofCents(19680),
                Money.ofCents(0));

        assertThat(templates.closed(envelope(closed)).body())
                .contains("No extra charges.")
                .contains("Final total: €196.80")
                .doesNotContain("Still owed");
    }

    private static <T> EventEnvelope<T> envelope(T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), payload.getClass().getSimpleName(), 1, Instant.now(),
                RESERVATION.toString(), payload);
    }
}
