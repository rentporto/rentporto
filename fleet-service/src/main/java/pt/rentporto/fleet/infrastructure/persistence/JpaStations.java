package pt.rentporto.fleet.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pt.rentporto.fleet.domain.Station;
import pt.rentporto.fleet.domain.Stations;

/**
 * Stations in PostgreSQL.
 */
@Repository
class JpaStations implements Stations {

    private final SpringDataStations jpa;

    JpaStations(SpringDataStations jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Station> all() {
        return jpa.findAll(Sort.by("code"));
    }
}
