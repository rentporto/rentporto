package pt.rentporto.rental.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;

/**
 * Agreements in memory.
 */
final class InMemoryAgreements implements RentalAgreements {

    private final Map<UUID, RentalAgreement> byId = new HashMap<>();

    @Override
    public Optional<RentalAgreement> withId(UUID rentalId) {
        return Optional.ofNullable(byId.get(rentalId));
    }

    @Override
    public Optional<RentalAgreement> lockWithId(UUID rentalId) {
        return withId(rentalId);
    }

    @Override
    public Optional<RentalAgreement> forReservation(UUID reservationId) {
        return byId.values().stream().filter(agreement -> agreement.reservationId().equals(reservationId)).findFirst();
    }

    @Override
    public void add(RentalAgreement agreement) {
        byId.put(agreement.id(), agreement);
    }

    @Override
    public void update(RentalAgreement agreement) {
        byId.put(agreement.id(), agreement);
    }
}
