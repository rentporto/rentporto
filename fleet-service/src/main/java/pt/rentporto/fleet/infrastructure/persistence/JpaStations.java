package pt.rentporto.fleet.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pt.rentporto.fleet.domain.model.Station;
import pt.rentporto.fleet.domain.repository.Stations;

/**
 * Stations in PostgreSQL. The lock is a {@code SELECT … FOR UPDATE} on the row of the station.
 */
@Repository
class JpaStations implements Stations {

    private final SpringDataStations jpa;
    private final EntityManager entityManager;

    JpaStations(SpringDataStations jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    @Override
    public List<Station> all() {
        return jpa.findAll(Sort.by("code"));
    }

    @Override
    public Optional<Station> withCode(String code) {
        return jpa.findById(code);
    }

    @Override
    public Optional<Station> lock(String code) {
        return Optional.ofNullable(entityManager.find(Station.class, code, LockModeType.PESSIMISTIC_WRITE));
    }
}
