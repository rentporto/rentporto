package pt.rentporto.events;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import pt.rentporto.events.customer.CustomerRegistered;
import pt.rentporto.events.fleet.StationCapacityChanged;
import pt.rentporto.events.fleet.VehicleCategoryChanged;
import pt.rentporto.events.payment.DepositSettled;
import pt.rentporto.events.payment.PaymentFailed;
import pt.rentporto.events.payment.PaymentRefunded;
import pt.rentporto.events.payment.SettleDeposit;
import pt.rentporto.events.rental.Damage;
import pt.rentporto.events.rental.ExtraCharge;
import pt.rentporto.events.rental.RentalClosed;
import pt.rentporto.events.rental.RentalStarted;
import pt.rentporto.events.rental.VehicleReturned;
import pt.rentporto.events.reservation.ContractExtra;
import pt.rentporto.events.reservation.ContractTerms;
import pt.rentporto.events.reservation.ReservationCancelled;
import pt.rentporto.events.reservation.ReservationConfirmed;
import pt.rentporto.events.reservation.ReservationExpired;
import pt.rentporto.events.reservation.ReservationNoShow;

/**
 * One example of every external command and event, wrapped in its envelope. Each one has a reference JSON in
 * {@code src/test/resources/messages/{type}.json}.
 */
final class SampleMessages {

    private static final UUID EVENT_ID = UUID.fromString("7d9f2c4e-1b3a-4c5d-8e6f-0a1b2c3d4e5f");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-01T09:00:00Z");

    private static final UUID CUSTOMER_ID = UUID.fromString("3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b");
    private static final UUID RESERVATION_ID = UUID.fromString("9b8a7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
    private static final UUID RENTAL_ID = UUID.fromString("5c4b3a29-1807-4f6e-9d5c-4b3a29180706");
    private static final UUID PAYMENT_ID = UUID.fromString("2e1d0c9b-8a79-4685-b4a3-92817f6e5d4c");
    private static final UUID DEPOSIT_ID = UUID.fromString("8f7e6d5c-4b3a-4291-8f7e-6d5c4b3a2918");

    private SampleMessages() {
    }

    static List<EventEnvelope<?>> all() {
        return List.of(
                outsideAReservation(new CustomerRegistered(CUSTOMER_ID, "Ana Ferreira", "ana.ferreira@example.com")),
                outsideAReservation(new StationCapacityChanged("OPO", "ECMR", 12)),
                outsideAReservation(new VehicleCategoryChanged("ECMR", "Group B - Economy", 21, 2)),
                ofTheReservation(reservationConfirmed()),
                ofTheReservation(new ReservationCancelled(
                        RESERVATION_ID, CUSTOMER_ID, Money.ofCents(0), Money.ofCents(19680))),
                ofTheReservation(new ReservationExpired(RESERVATION_ID, CUSTOMER_ID, "PAYMENT_TIMEOUT")),
                ofTheReservation(new ReservationNoShow(RESERVATION_ID, CUSTOMER_ID, Money.ofCents(4500))),
                ofTheReservation(new SettleDeposit(RENTAL_ID, DEPOSIT_ID, Money.ofCents(20080))),
                ofTheReservation(new PaymentFailed(PAYMENT_ID, RESERVATION_ID, CUSTOMER_ID, "CARD_DECLINED")),
                ofTheReservation(new PaymentRefunded(PAYMENT_ID, RESERVATION_ID, CUSTOMER_ID, Money.ofCents(19680))),
                ofTheReservation(new DepositSettled(
                        RENTAL_ID, DEPOSIT_ID, Money.ofCents(20080), Money.ofCents(39920), Money.ofCents(0))),
                ofTheReservation(new RentalStarted(
                        RENTAL_ID, RESERVATION_ID, CUSTOMER_ID, "AA-12-BB", OffsetDateTime.parse("2026-07-01T10:05:00+01:00"))),
                ofTheReservation(new VehicleReturned(
                        RENTAL_ID, RESERVATION_ID, "AA-12-BB", "OPO", 48230, 6,
                        List.of(new Damage("FRONT_BUMPER", "10 cm scratch")))),
                ofTheReservation(rentalClosed()));
    }

    private static ReservationConfirmed reservationConfirmed() {
        RentalPeriod period = new RentalPeriod(
                OffsetDateTime.parse("2026-07-01T10:00:00+01:00"),
                OffsetDateTime.parse("2026-07-04T10:00:00+01:00"));
        ContractTerms terms = new ContractTerms(
                Money.ofCents(4500),
                List.of(new ContractExtra("CHILD_SEAT", Money.ofCents(500), Money.ofCents(2500))),
                "MEDIUM",
                Money.ofCents(100000),
                Money.ofCents(60000),
                Money.ofCents(215),
                Money.ofCents(2500));
        return new ReservationConfirmed(
                RESERVATION_ID, CUSTOMER_ID, "OPO", "OPO", "ECMR", period, terms, Money.ofCents(19680));
    }

    private static RentalClosed rentalClosed() {
        List<ExtraCharge> charges = List.of(
                new ExtraCharge(ExtraCharge.Type.FUEL, "12 litres missing", Money.ofCents(2580)),
                new ExtraCharge(ExtraCharge.Type.FUEL, "Refuelling fee", Money.ofCents(2500)),
                new ExtraCharge(ExtraCharge.Type.DAMAGE, "10 cm scratch on the front bumper", Money.ofCents(15000)));
        return new RentalClosed(RENTAL_ID, CUSTOMER_ID, charges, Money.ofCents(39760), Money.ofCents(0));
    }

    private static <T> EventEnvelope<T> ofTheReservation(T payload) {
        return envelope(payload, RESERVATION_ID.toString());
    }

    private static <T> EventEnvelope<T> outsideAReservation(T payload) {
        return envelope(payload, EVENT_ID.toString());
    }

    private static <T> EventEnvelope<T> envelope(T payload, String correlationId) {
        return new EventEnvelope<>(EVENT_ID, payload.getClass().getSimpleName(), 1, OCCURRED_AT, correlationId, payload);
    }
}
