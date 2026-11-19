package pt.rentporto.rental.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.payment.DepositSettled;
import pt.rentporto.messaging.consumer.IdempotentConsumer;
import pt.rentporto.rental.application.Settlements;
import pt.rentporto.rental.domain.Money;
import pt.rentporto.rental.domain.Settlement;

/**
 * Consumes the events of the payment module of the booking-service. Each event is applied once, in the transaction
 * that records it as handled (ADR-07); one that cannot be applied fails its three attempts and goes to the dead letter
 * queue.
 */
@Component
class PaymentEventsListener {

    static final String DEPOSIT_SETTLED = "rental.deposit-settled";

    private final IdempotentConsumer consumer;
    private final Settlements settlements;

    PaymentEventsListener(IdempotentConsumer consumer, Settlements settlements) {
        this.consumer = consumer;
        this.settlements = settlements;
    }

    @RabbitListener(queues = DEPOSIT_SETTLED)
    void onDepositSettled(EventEnvelope<DepositSettled> message) {
        consumer.process(DEPOSIT_SETTLED, message, settled -> settlements.depositSettled(
                settled.rentalId(),
                settled.depositId(),
                new Settlement(
                        Money.ofCents(settled.captured().amountCents()),
                        Money.ofCents(settled.released().amountCents()),
                        Money.ofCents(settled.outstanding().amountCents()))));
    }
}
