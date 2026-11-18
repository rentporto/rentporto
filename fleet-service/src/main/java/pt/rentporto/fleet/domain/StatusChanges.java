package pt.rentporto.fleet.domain;

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
