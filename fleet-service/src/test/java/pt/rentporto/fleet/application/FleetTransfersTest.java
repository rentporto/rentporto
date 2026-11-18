package pt.rentporto.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.StationCapacity;
import pt.rentporto.fleet.domain.StatusChange;
import pt.rentporto.fleet.domain.Transfer;
import pt.rentporto.fleet.domain.TransferNotFoundException;
import pt.rentporto.fleet.domain.TransferStatus;
import pt.rentporto.fleet.domain.Transfers;
import pt.rentporto.fleet.domain.UnknownStationException;
import pt.rentporto.fleet.domain.Vehicle;
import pt.rentporto.fleet.domain.VehicleNotFoundException;
import pt.rentporto.fleet.domain.VehicleStatus;

class FleetTransfersTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneId.of("Europe/Lisbon"));
    private static final AcrissCode ECONOMY = new AcrissCode("ECMR");
    private static final LicensePlate FIRST = new LicensePlate("AA-12-BB");
    private static final LicensePlate SECOND = new LicensePlate("CC-34-DD");

    private final InMemoryVehicles vehicles = new InMemoryVehicles();
    private final LockedStations stations = new LockedStations();
    private final InMemoryTransfers transfers = new InMemoryTransfers();
    private final List<StatusChange> history = new ArrayList<>();
    private final RecordedEvents events = new RecordedEvents();
    private final FleetTransfers fleet = new FleetTransfers(
            vehicles, stations, transfers, history::add, events, CLOCK, mock(PlatformTransactionManager.class));

    @BeforeEach
    void setUp() {
        vehicles.add(Vehicle.register(FIRST, ECONOMY, "OPO", 12000, null));
        vehicles.add(Vehicle.register(SECOND, ECONOMY, "OPO", 500, null));
    }

    @Test
    void startingATransferTakesTheVehicleOutOfTheOriginAndPublishesOnlyItsCapacity() {
        Transfer transfer = fleet.start(FIRST, "MAT", null, "frota");

        assertThat(transfer.status()).isEqualTo(TransferStatus.IN_TRANSFER);
        assertThat(vehicles.withPlate(FIRST).orElseThrow().status()).isEqualTo(VehicleStatus.IN_TRANSFER);
        assertThat(events.capacities).containsExactly(new StationCapacity("OPO", ECONOMY, 1));
        assertThat(stations.locked).containsExactly("OPO");
        assertThat(history).singleElement()
                .extracting(StatusChange::from, StatusChange::to, StatusChange::reason, StatusChange::changedBy)
                .containsExactly(VehicleStatus.AVAILABLE, VehicleStatus.IN_TRANSFER, "Transfer " + transfer.id(),
                        "frota");
    }

    @Test
    void completingATransferMakesTheVehicleAvailableAtTheDestinationAndPublishesOnlyItsCapacity() {
        Transfer transfer = fleet.start(FIRST, "MAT", null, "frota");

        Transfer completed = fleet.complete(FIRST, transfer.id(), "rui");

        assertThat(completed.status()).isEqualTo(TransferStatus.COMPLETED);
        Vehicle vehicle = vehicles.withPlate(FIRST).orElseThrow();
        assertThat(vehicle.status()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.station()).isEqualTo("MAT");
        assertThat(events.capacities).containsExactly(
                new StationCapacity("OPO", ECONOMY, 1), new StationCapacity("MAT", ECONOMY, 1));
        assertThat(stations.locked).containsExactly("OPO", "MAT");
        assertThat(history.getLast())
                .extracting(StatusChange::from, StatusChange::to, StatusChange::changedBy)
                .containsExactly(VehicleStatus.IN_TRANSFER, VehicleStatus.AVAILABLE, "rui");
    }

    @Test
    void completingATransferAgainChangesNothing() {
        Transfer transfer = fleet.start(FIRST, "MAT", null, "frota");
        fleet.complete(FIRST, transfer.id(), "rui");

        Transfer again = fleet.complete(FIRST, transfer.id(), "rui");

        assertThat(again.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(events.capacities).hasSize(2);
        assertThat(history).hasSize(2);
    }

    @Test
    void theSameKeyGetsTheSameTransferEvenAfterItIsCompleted() {
        Transfer transfer = fleet.start(FIRST, "MAT", "key-1", "frota");

        assertThat(fleet.start(FIRST, "MAT", "key-1", "frota")).isSameAs(transfer);
        fleet.complete(FIRST, transfer.id(), "rui");
        assertThat(fleet.start(FIRST, "MAT", "key-1", "frota")).isSameAs(transfer);
        assertThat(events.capacities).hasSize(2);
    }

    @Test
    void aDestinationThatDoesNotExistChangesNothing() {
        assertThatThrownBy(() -> fleet.start(FIRST, "BRG", null, "frota"))
                .isInstanceOf(UnknownStationException.class);
        assertThat(vehicles.withPlate(FIRST).orElseThrow().status()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(events.capacities).isEmpty();
    }

    @Test
    void transferringAVehicleThatDoesNotExistIsNotFound() {
        assertThatThrownBy(() -> fleet.start(new LicensePlate("ZZ-99-ZZ"), "MAT", null, "frota"))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void aTransferOfAnotherVehicleOrThatDoesNotExistIsNotFound() {
        Transfer transfer = fleet.start(FIRST, "MAT", null, "frota");

        assertThatThrownBy(() -> fleet.complete(SECOND, transfer.id(), "rui"))
                .isInstanceOf(TransferNotFoundException.class);
        assertThatThrownBy(() -> fleet.complete(FIRST, UUID.randomUUID(), "rui"))
                .isInstanceOf(TransferNotFoundException.class);
        assertThat(transfer.status()).isEqualTo(TransferStatus.IN_TRANSFER);
    }

    /**
     * Transfers in memory.
     */
    private static final class InMemoryTransfers implements Transfers {

        private final Map<UUID, Transfer> byId = new HashMap<>();

        @Override
        public Optional<Transfer> withId(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Transfer> startedWith(LicensePlate plate, String key) {
            return byId.values().stream()
                    .filter(transfer -> transfer.plate().equals(plate) && transfer.startedWith(key))
                    .findFirst();
        }

        @Override
        public void add(Transfer transfer) {
            byId.put(transfer.id(), transfer);
        }

        @Override
        public void update(Transfer transfer) {
            byId.put(transfer.id(), transfer);
        }
    }
}
