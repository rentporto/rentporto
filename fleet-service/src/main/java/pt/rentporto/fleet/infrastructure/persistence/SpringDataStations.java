package pt.rentporto.fleet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.fleet.domain.Station;

interface SpringDataStations extends JpaRepository<Station, String> {
}
