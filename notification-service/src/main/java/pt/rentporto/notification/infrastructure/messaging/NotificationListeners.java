package pt.rentporto.notification.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.customer.CustomerRegistered;
import pt.rentporto.events.payment.PaymentFailed;
import pt.rentporto.events.payment.PaymentRefunded;
import pt.rentporto.events.rental.RentalClosed;
import pt.rentporto.events.rental.RentalStarted;
import pt.rentporto.events.reservation.ReservationCancelled;
import pt.rentporto.events.reservation.ReservationConfirmed;
import pt.rentporto.events.reservation.ReservationExpired;
import pt.rentporto.events.reservation.ReservationNoShow;
import pt.rentporto.notification.application.CustomerEmails;

/**
 * Consumes the nine events the customers hear of, each from its own queue, and emails them. An event that cannot be
 * sent, because the customer is not known yet or the mail server failed, is tried again and goes to the dead letter
 * queue of its queue after its attempts.
 */
@Component
class NotificationListeners {

    static final String CUSTOMER_REGISTERED = "notification.customer-registered";
    static final String RESERVATION_CONFIRMED = "notification.reservation-confirmed";
    static final String RESERVATION_CANCELLED = "notification.reservation-cancelled";
    static final String RESERVATION_EXPIRED = "notification.reservation-expired";
    static final String RESERVATION_NO_SHOW = "notification.reservation-no-show";
    static final String PAYMENT_FAILED = "notification.payment-failed";
    static final String PAYMENT_REFUNDED = "notification.payment-refunded";
    static final String RENTAL_STARTED = "notification.rental-started";
    static final String RENTAL_CLOSED = "notification.rental-closed";

    private final CustomerEmails emails;
    private final EmailTemplates templates;

    NotificationListeners(CustomerEmails emails, EmailTemplates templates) {
        this.emails = emails;
        this.templates = templates;
    }

    @RabbitListener(queues = CUSTOMER_REGISTERED)
    void onCustomerRegistered(EventEnvelope<CustomerRegistered> event) {
        CustomerRegistered registered = event.payload();
        emails.customerRegistered(
                registered.customerId(), registered.fullName(), registered.email(), templates.welcome(event));
    }

    @RabbitListener(queues = RESERVATION_CONFIRMED)
    void onReservationConfirmed(EventEnvelope<ReservationConfirmed> event) {
        emails.send(templates.confirmed(event));
    }

    @RabbitListener(queues = RESERVATION_CANCELLED)
    void onReservationCancelled(EventEnvelope<ReservationCancelled> event) {
        emails.send(templates.cancelled(event));
    }

    @RabbitListener(queues = RESERVATION_EXPIRED)
    void onReservationExpired(EventEnvelope<ReservationExpired> event) {
        emails.send(templates.expired(event));
    }

    @RabbitListener(queues = RESERVATION_NO_SHOW)
    void onReservationNoShow(EventEnvelope<ReservationNoShow> event) {
        emails.send(templates.noShow(event));
    }

    @RabbitListener(queues = PAYMENT_FAILED)
    void onPaymentFailed(EventEnvelope<PaymentFailed> event) {
        emails.send(templates.paymentFailed(event));
    }

    @RabbitListener(queues = PAYMENT_REFUNDED)
    void onPaymentRefunded(EventEnvelope<PaymentRefunded> event) {
        emails.send(templates.refunded(event));
    }

    @RabbitListener(queues = RENTAL_STARTED)
    void onRentalStarted(EventEnvelope<RentalStarted> event) {
        emails.send(templates.started(event));
    }

    @RabbitListener(queues = RENTAL_CLOSED)
    void onRentalClosed(EventEnvelope<RentalClosed> event) {
        emails.send(templates.closed(event));
    }
}
