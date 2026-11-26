package pt.rentporto.fleet.domain.repository;

import java.util.Optional;
import java.util.UUID;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.Transfer;

/**
 * Stored transfers. Implemented in the infrastructure.
 */
public interface Transfers {

    /**
     * Finds a transfer.
     *
     * @param id transfer identifier
     * @return the transfer, if there is one
     */
    Optional<Transfer> withId(UUID id);

    /**
     * Finds the transfer of a vehicle started by a request with an idempotency key.
     *
     * @param plate licence plate of the vehicle
     * @param key   idempotency key of the request
     * @return the transfer, if there is one
     */
    Optional<Transfer> startedWith(LicensePlate plate, String key);

    /**
     * Stores a new transfer.
     *
     * @param transfer the transfer
     */
    void add(Transfer transfer);

    /**
     * Stores the changes to a transfer.
     *
     * @param transfer the transfer
     */
    void update(Transfer transfer);
}
