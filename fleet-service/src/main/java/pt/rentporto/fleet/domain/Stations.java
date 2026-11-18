package pt.rentporto.fleet.domain;

import java.util.List;

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
}
