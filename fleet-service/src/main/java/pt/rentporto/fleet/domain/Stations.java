package pt.rentporto.fleet.domain;

import java.util.List;
import java.util.Optional;

/**
 * Stored stations. Implemented in the infrastructure.
 */
public interface Stations {

    /**
     * Lists the stations.
     *
     * @return every station, by code
     */
    List<Station> all();

    /**
     * Finds a station and locks it until the end of the transaction. Every change to the capacity of a station takes
     * this lock before counting, so that two changes at the same time publish the right values, in order.
     *
     * @param code code of the station
     * @return the station, if there is one
     */
    Optional<Station> lock(String code);
}
