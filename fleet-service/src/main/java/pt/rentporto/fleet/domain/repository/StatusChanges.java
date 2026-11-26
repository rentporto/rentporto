package pt.rentporto.fleet.domain.repository;

import pt.rentporto.fleet.domain.model.StatusChange;

/**
 * Stored history of the states of the vehicles. Implemented in the infrastructure.
 */
public interface StatusChanges {

    /**
     * Stores an entry.
     *
     * @param change the entry
     */
    void add(StatusChange change);
}
