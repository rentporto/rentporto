package pt.rentporto.notification.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Money;
import pt.rentporto.events.customer.CustomerRegistered;
import pt.rentporto.events.payment.PaymentFailed;
import pt.rentporto.events.payment.PaymentRefunded;
import pt.rentporto.events.rental.RentalClosed;
import pt.rentporto.events.rental.RentalStarted;
import pt.rentporto.events.reservation.ReservationCancelled;
import pt.rentporto.events.reservation.ReservationConfirmed;
import pt.rentporto.events.reservation.ReservationExpired;
import pt.rentporto.events.reservation.ReservationNoShow;
import pt.rentporto.notification.application.model.Notice;
import pt.rentporto.notification.application.port.ReminderTemplate;
import pt.rentporto.notification.domain.model.PickupReminder;

/**
 * The plain text emails of the events and of the pick-up reminder, in English like every text the customers see, with
 * the amounts in euro and the dates in Lisbon.
 */
@Component
class EmailTemplates implements ReminderTemplate {

    private static final String PICKUP_REMINDER = "PickupReminder";
    private static final String SIGNATURE = "\n\nRentPorto";

    private final DateTimeFormatter dates;

    EmailTemplates(Clock clock) {
        this.dates = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).withZone(clock.getZone());
    }

    Notice welcome(EventEnvelope<CustomerRegistered> event) {
        CustomerRegistered registered = event.payload();
        return notice(event, registered.customerId(), "Welcome to RentPorto",
                "Hello " + registered.fullName() + ",\n\n"
                        + "Your profile is ready: you can now book a vehicle at any of our stations.");
    }

    Notice confirmed(EventEnvelope<ReservationConfirmed> event) {
        ReservationConfirmed confirmed = event.payload();
        return notice(event, confirmed.customerId(), "Your reservation is confirmed",
                "Your reservation " + confirmed.reservationId() + " is confirmed.\n\n"
                        + "Pick-up: " + confirmed.pickupStation() + ", " + date(confirmed.rentalPeriod().pickupAt())
                        + "\nReturn: " + confirmed.returnStation() + ", " + date(confirmed.rentalPeriod().returnAt())
                        + "\nCategory: " + confirmed.categoryCode()
                        + "\nPaid: " + euros(confirmed.total())
                        + "\nSecurity deposit held at pick-up: " + euros(confirmed.contractTerms().deposit()));
    }

    Notice cancelled(EventEnvelope<ReservationCancelled> event) {
        ReservationCancelled cancelled = event.payload();
        return notice(event, cancelled.customerId(), "Your reservation was cancelled",
                "Your reservation " + cancelled.reservationId() + " was cancelled.\n\n"
                        + "Cancellation fee: " + euros(cancelled.cancellationFee())
                        + "\nRefund: " + euros(cancelled.refundAmount()));
    }

    Notice expired(EventEnvelope<ReservationExpired> event) {
        ReservationExpired expired = event.payload();
        return notice(event, expired.customerId(), "Your reservation expired",
                "Your reservation " + expired.reservationId() + " expired before it was confirmed ("
                        + expired.reason() + "). Nothing was charged.");
    }

    Notice noShow(EventEnvelope<ReservationNoShow> event) {
        ReservationNoShow noShow = event.payload();
        return notice(event, noShow.customerId(), "You did not pick up your vehicle",
                "The vehicle of your reservation " + noShow.reservationId() + " was not picked up.\n\n"
                        + "Amount kept: " + euros(noShow.retainedAmount())
                        + "\nThe rest of your payment is refunded.");
    }

    Notice paymentFailed(EventEnvelope<PaymentFailed> event) {
        PaymentFailed failed = event.payload();
        return notice(event, failed.customerId(), "Your payment failed",
                "The payment of your reservation " + failed.reservationId() + " failed (" + failed.reason()
                        + "), so the reservation was not confirmed.");
    }

    Notice refunded(EventEnvelope<PaymentRefunded> event) {
        PaymentRefunded refunded = event.payload();
        return notice(event, refunded.customerId(), "Your refund is on its way",
                "We refunded " + euros(refunded.amount()) + " for your reservation " + refunded.reservationId()
                        + ".");
    }

    Notice started(EventEnvelope<RentalStarted> event) {
        RentalStarted started = event.payload();
        return notice(event, started.customerId(), "Enjoy your trip",
                "You picked up the vehicle " + started.licensePlate() + " on " + date(started.pickedUpAt())
                        + ".\nRental: " + started.rentalId());
    }

    Notice closed(EventEnvelope<RentalClosed> event) {
        RentalClosed closed = event.payload();
        String charges = closed.extraCharges().isEmpty()
                ? "No extra charges."
                : "Extra charges:\n" + closed.extraCharges().stream()
                        .map(charge -> "- " + charge.description() + ": " + euros(charge.amount()))
                        .collect(Collectors.joining("\n"));
        String owed = closed.outstanding().amountCents() > 0
                ? "\nStill owed: " + euros(closed.outstanding())
                : "";
        return notice(event, closed.customerId(), "Your receipt",
                "Your rental " + closed.rentalId() + " is closed.\n\n" + charges
                        + "\n\nFinal total: " + euros(closed.finalTotal()) + owed);
    }

    @Override
    public Notice pickupReminder(PickupReminder reminder) {
        return new Notice(reminder.noticeId(), reminder.customerId(), PICKUP_REMINDER, "Your pick-up is coming up",
                "Your vehicle for the reservation " + reminder.reservationId() + " is waiting for you.\n\n"
                        + "Pick-up: " + reminder.pickupStation() + ", " + dates.format(reminder.pickupAt())
                        + "\nBring your driving licence and a payment card for the security deposit." + SIGNATURE);
    }

    private static Notice notice(EventEnvelope<?> event, UUID customerId, String subject, String text) {
        return new Notice(event.eventId(), customerId, event.type(), subject, text + SIGNATURE);
    }

    private String date(OffsetDateTime instant) {
        return dates.format(instant);
    }

    private static String euros(Money money) {
        return "€" + BigDecimal.valueOf(money.amountCents(), 2).toPlainString();
    }
}
