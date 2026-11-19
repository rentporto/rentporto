package pt.rentporto.rental.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalNotFoundException;
import pt.rentporto.rental.domain.RentalStatus;

/**
 * Reads the rental agreements: the agents see all of them, a customer only their own.
 */
@Service
public class RentalQueries {

    private final RentalAgreements agreements;

    RentalQueries(RentalAgreements agreements) {
        this.agreements = agreements;
    }

    /**
     * Lists the agreements that go through a station, picked up or returned there, in a state.
     *
     * @param station code of the station, or {@code null} for every station
     * @param status  the state, or {@code null} for every state
     * @return the agreements, by pick-up time
     */
    public List<RentalAgreement> agreements(String station, RentalStatus status) {
        return agreements.matching(station, status);
    }

    /**
     * Reads an agreement, as an agent.
     *
     * @param rentalId identifier of the agreement
     * @return the agreement
     * @throws RentalNotFoundException if there is no agreement with the identifier
     */
    public RentalAgreement agreement(UUID rentalId) {
        return agreements.withId(rentalId).orElseThrow(() -> new RentalNotFoundException(rentalId));
    }

    /**
     * Reads an agreement of a customer. The agreement of another customer is not found, so its existence is not
     * revealed.
     *
     * @param rentalId   identifier of the agreement
     * @param customerId the customer who asks
     * @return the agreement
     * @throws RentalNotFoundException if the customer has no agreement with the identifier
     */
    public RentalAgreement agreementOf(UUID rentalId, UUID customerId) {
        return agreements.withId(rentalId)
                .filter(agreement -> agreement.customerId().equals(customerId))
                .orElseThrow(() -> new RentalNotFoundException(rentalId));
    }
}
