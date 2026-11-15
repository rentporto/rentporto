package pt.rentporto.events;

/**
 * RabbitMQ exchange names shared between services.
 */
public final class Exchanges {

    /** Topic exchange for all events; routing key is {@code {context}.{event}}, e.g. {@code reservation.confirmed}. */
    public static final String EVENTS = "rentporto.events";

    private Exchanges() {
    }
}
