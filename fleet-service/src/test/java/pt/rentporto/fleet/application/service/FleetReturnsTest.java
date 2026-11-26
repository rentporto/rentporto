package pt.rentporto.fleet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.fleet.domain.exception.InconsistentReturnException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.StationCapacity;
import pt.rentporto.fleet.domain.model.StatusChange;
import pt.rentporto.fleet.domain.model.Vehicle;
import pt.rentporto.fleet.domain.model.VehicleStatus;

class FleetReturnsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T18:00:00Z"), ZoneId.of("Europe/Lisbon"));
    private static final AcrissCode ECONOMY = new AcrissCode("ECMR");
    private static final LicensePlate RENTED = new LicensePlate("AA-12-BB");
    private static final LicensePlate OTHER = new LicensePlate("CC-34-DD");
    private static final UUID RENTAL = UUID.fromString("5c4b3a29-1807-4f6e-9d5c-4b3a29180706");

    private final InMemoryVehicles vehicles = new InMemoryVehicles();
    private final InMemoryAssignments assignments = new InMemoryAssignments();
    private final LockedStations stations = new LockedStations();
    private final List<StatusChange> history = new ArrayList<>();
    private final RecordedEvents events = new RecordedEvents();
    private final FleetReturns returns = new FleetReturns(
            vehicles, assignments, stations, history::add, events, CLOCK, mock(PlatformTransactionManager.class));

    @BeforeEach
    void setUp() {
        Vehicle vehicle = Vehicle.register(RENTED, ECONOMY, "OPO", 12000, null);
        vehicles.add(vehicle);
        vehicles.add(Vehicle.register(OTHER, ECONOMY, "OPO", 500, null));
        vehicles.add(Vehicle.register(new LicensePlate("EE-56-FF"), ECONOMY, "MAT", 800, null));
        assignments.add(vehicle.assignTo(RENTAL, "key-1"));
    }

    @Test
    void aReturnAtThePickUpStationUpdatesTheVehicleWithoutChangingTheCapacity() {
        assertThat(returns.vehicleReturned(RENTAL, RENTED, "OPO", 12450, 6)).isTrue();

        Vehicle vehicle = vehicles.withPlate(RENTED).orElseThrow();
        assertThat(vehicle.status()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.station()).isEqualTo("OPO");
        assertThat(vehicle.odometerKm()).isEqualTo(12450);
        assertThat(vehicle.fuelLevelEighths()).isEqualTo(6);
        assertThat(assignments.forRental(RENTAL).orElseThrow().returned()).isTrue();
        assertThat(events.capacities).isEmpty();
        assertThat(stations.locked).isEmpty();
        assertThat(history).singleElement()
                .extracting(StatusChange::from, StatusChange::to, StatusChange::reason, StatusChange::changedBy)
                .containsExactly(VehicleStatus.RENTED, VehicleStatus.AVAILABLE, "Rental " + RENTAL + " returned",
                        "rental-service");
    }

    @Test
    void aOneWayReturnPublishesThePickUpStationBeforeTheReturnStationWithBothLockedInOrder() {
        returns.vehicleReturned(RENTAL, RENTED, "MAT", 12450, 6);

        assertThat(vehicles.withPlate(RENTED).orElseThrow().station()).isEqualTo("MAT");
        assertThat(events.capacities).containsExactly(
                new StationCapacity("OPO", ECONOMY, 1), new StationCapacity("MAT", ECONOMY, 2));
        assertThat(stations.locked).containsExactly("MAT", "OPO");
        assertThat(assignments.lockedBeforeLookingFor).containsExactly(RENTAL);
    }

    @Test
    void aSecondReturnOfTheRentalChangesNothing() {
        returns.vehicleReturned(RENTAL, RENTED, "MAT", 12450, 6);

        assertThat(returns.vehicleReturned(RENTAL, RENTED, "TRD", 13000, 2)).isFalse();

        assertThat(vehicles.withPlate(RENTED).orElseThrow().station()).isEqualTo("MAT");
        assertThat(events.capacities).hasSize(2);
        assertThat(history).hasSize(1);
    }

    @Test
    void aRentalWithoutAVehicleOrWithAnotherVehicleIsInconsistent() {
        assertThatThrownBy(() -> returns.vehicleReturned(UUID.randomUUID(), RENTED, "OPO", 12450, 6))
                .isInstanceOf(InconsistentReturnException.class);
        assertThatThrownBy(() -> returns.vehicleReturned(RENTAL, OTHER, "OPO", 12450, 6))
                .isInstanceOf(InconsistentReturnException.class);
        assertThat(vehicles.withPlate(RENTED).orElseThrow().status()).isEqualTo(VehicleStatus.RENTED);
        assertThat(vehicles.withPlate(OTHER).orElseThrow().status()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void aStationThatDoesNotExistOrAnOdometerThatWentBackIsInconsistent() {
        assertThatThrownBy(() -> returns.vehicleReturned(RENTAL, RENTED, "BRG", 12450, 6))
                .isInstanceOf(InconsistentReturnException.class);
        assertThatThrownBy(() -> returns.vehicleReturned(RENTAL, RENTED, "OPO", 11000, 6))
                .isInstanceOf(InconsistentReturnException.class);
        assertThat(vehicles.withPlate(RENTED).orElseThrow().status()).isEqualTo(VehicleStatus.RENTED);
        assertThat(assignments.forRental(RENTAL).orElseThrow().returned()).isFalse();
        assertThat(events.capacities).isEmpty();
    }
}
