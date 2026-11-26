package pt.rentporto.rental.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.rental.application.port.RentalEvents;
import pt.rentporto.rental.domain.exception.InconsistentEventException;
import pt.rentporto.rental.domain.model.RentalAgreement;
import pt.rentporto.rental.domain.model.Settlement;
import pt.rentporto.rental.domain.repository.RentalAgreements;

/**
 * Closes the agreements whose deposit the payments settled, and publishes {@code RentalClosed} for the receipt.
 */
@Service
public class Settlements {

    private final RentalAgreements agreements;
    private final RentalEvents events;
    private final TransactionTemplate transaction;

    Settlements(RentalAgreements agreements, RentalEvents events, PlatformTransactionManager transactionManager) {
        this.agreements = agreements;
        this.events = events;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Records the settlement of the deposit of an agreement and closes it.
     *
     * @param rentalId   the rental
     * @param depositId  the deposit that was settled
     * @param settlement how it was settled
     * @return {@code true} if the agreement was closed now, {@code false} if it already was or was discarded
     * @throws InconsistentEventException if the rental does not exist, has another deposit or was not returned yet
     */
    public boolean depositSettled(UUID rentalId, UUID depositId, Settlement settlement) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            RentalAgreement agreement = agreements.lockWithId(rentalId)
                    .orElseThrow(() -> new InconsistentEventException("There is no rental " + rentalId));
            if (!agreement.close(depositId, settlement)) {
                return false;
            }
            agreements.update(agreement);
            events.rentalClosed(agreement);
            return true;
        }));
    }
}
