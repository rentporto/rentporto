package pt.rentporto.events.topology;

/**
 * Routing keys of the events published on {@link Exchanges#EVENTS}.
 *
 * <p>The format is {@code {context}.{event}} in kebab-case. The prefix is the bounded context, not the service,
 * so splitting a module out of the booking-service does not change any consumer.
 */
public final class RoutingKeys {

    public static final String CUSTOMER_REGISTERED = "customer.registered";

    public static final String STATION_CAPACITY_CHANGED = "fleet.station-capacity-changed";
    public static final String VEHICLE_CATEGORY_CHANGED = "fleet.vehicle-category-changed";

    public static final String RESERVATION_CONFIRMED = "reservation.confirmed";
    public static final String RESERVATION_CANCELLED = "reservation.cancelled";
    public static final String RESERVATION_EXPIRED = "reservation.expired";
    public static final String RESERVATION_NO_SHOW = "reservation.no-show";

    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUNDED = "payment.refunded";
    public static final String DEPOSIT_SETTLED = "payment.deposit-settled";

    public static final String RENTAL_STARTED = "rental.started";
    public static final String VEHICLE_RETURNED = "rental.vehicle-returned";
    public static final String RENTAL_CLOSED = "rental.closed";

    private RoutingKeys() {
    }
}
