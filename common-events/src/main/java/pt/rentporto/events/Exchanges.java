package pt.rentporto.events;

/**
 * RabbitMQ exchange names shared between services.
 */
public final class Exchanges {

    /** Topic exchange for all events; the routing keys are in {@link RoutingKeys}. */
    public static final String EVENTS = "rentporto.events";

    private Exchanges() {
    }
}
