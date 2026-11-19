package pt.rentporto.rental.application;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalStatus;

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
    public List<RentalAgreement> matching(String station, RentalStatus status) {
        return byId.values().stream()
                .filter(agreement -> station == null
                        || agreement.pickupStation().equals(station) || agreement.returnStation().equals(station))
                .filter(agreement -> status == null || agreement.status() == status)
                .sorted(Comparator.comparing(agreement -> agreement.period().pickupAt()))
                .toList();
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
