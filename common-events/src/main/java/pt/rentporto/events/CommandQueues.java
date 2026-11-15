package pt.rentporto.events;

/**
 * Queues of the commands, each with a single consumer.
 *
 * <p>Commands are sent through the default exchange, with the queue name as the routing key. Event queues are not
 * listed here: each consumer declares its own, named {@code {service}.{event}} (or {@code booking.{module}.{event}}
 * in the booking-service), with a {@code .dlq} counterpart.
 */
public final class CommandQueues {

    /** {@code SettleDeposit}, sent by the rental-service and consumed by the payment module of the booking-service. */
    public static final String SETTLE_DEPOSIT = "payment.settle-deposit";

    private CommandQueues() {
    }
}
