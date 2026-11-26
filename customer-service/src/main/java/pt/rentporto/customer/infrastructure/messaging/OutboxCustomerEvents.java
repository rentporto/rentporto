package pt.rentporto.customer.infrastructure.messaging;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pt.rentporto.customer.application.port.CustomerEvents;
import pt.rentporto.customer.domain.model.Customer;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.customer.CustomerRegistered;
import pt.rentporto.events.topology.RoutingKeys;
import pt.rentporto.messaging.outbox.OutboxWriter;

/**
 * Writes the events of the customer context to the outbox, which sends them to RabbitMQ after the commit (ADR-07).
 * The correlation id of an event is the id of the customer.
 */
@Component
class OutboxCustomerEvents implements CustomerEvents {

    private final OutboxWriter outbox;
    private final Clock clock;

    OutboxCustomerEvents(OutboxWriter outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    public void registered(Customer customer) {
        CustomerRegistered event = new CustomerRegistered(customer.id(), customer.fullName(), customer.email());
        outbox.publish(RoutingKeys.CUSTOMER_REGISTERED, new EventEnvelope<>(
                UUID.randomUUID(),
                CustomerRegistered.class.getSimpleName(),
                1,
                clock.instant(),
                customer.id().toString(),
                event));
    }
}
