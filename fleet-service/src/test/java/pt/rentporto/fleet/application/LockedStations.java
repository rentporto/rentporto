package pt.rentporto.fleet.application;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import pt.rentporto.fleet.domain.Station;
import pt.rentporto.fleet.domain.Stations;

/**
 * The four stations; remembers which ones were locked, in order.
 */
final class LockedStations implements Stations {

    private final Set<String> codes = Set.of("OPO", "CMP", "TRD", "MAT");
    final List<String> locked = new ArrayList<>();

    @Override
    public List<Station> all() {
        return List.of();
    }

    @Override
    public Optional<Station> withCode(String code) {
        return codes.contains(code) ? Optional.of(mock(Station.class)) : Optional.empty();
    }

    @Override
    public Optional<Station> lock(String code) {
        Optional<Station> station = withCode(code);
        station.ifPresent(found -> locked.add(code));
        return station;
    }
}
