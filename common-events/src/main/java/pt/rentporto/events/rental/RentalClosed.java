package pt.rentporto.events.rental;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * The rental agreement was settled and closed.
 *
 * <p>Published by the rental-service on {@code rental.closed}; consumed by the notification-service, which sends
 * the receipt.
 *
 * @param rentalId     rental agreement identifier
 * @param customerId   customer who rented the vehicle
 * @param extraCharges charges on top of the advance payment, possibly none
 * @param finalTotal   advance payment plus extra charges
 * @param outstanding  amount still owed by the customer; zero when everything was charged
 */
public record RentalClosed(
        UUID rentalId,
        UUID customerId,
        List<ExtraCharge> extraCharges,
        Money finalTotal,
        Money outstanding) {

    public RentalClosed {
        Objects.requireNonNull(rentalId, "rentalId");
        Objects.requireNonNull(customerId, "customerId");
        extraCharges = List.copyOf(Objects.requireNonNull(extraCharges, "extraCharges"));
        Objects.requireNonNull(finalTotal, "finalTotal");
        Objects.requireNonNull(outstanding, "outstanding");
    }
}
