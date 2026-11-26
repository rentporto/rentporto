package pt.rentporto.fleet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.fleet.domain.exception.InvalidStatusTransitionException;
import pt.rentporto.fleet.domain.exception.UnknownCategoryException;
import pt.rentporto.fleet.domain.exception.UnknownStationException;
import pt.rentporto.fleet.domain.exception.VehicleAlreadyRegisteredException;
import pt.rentporto.fleet.domain.exception.VehicleNotFoundException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.StationCapacity;
import pt.rentporto.fleet.domain.model.StatusChange;
import pt.rentporto.fleet.domain.model.Vehicle;
import pt.rentporto.fleet.domain.model.VehicleCategory;
import pt.rentporto.fleet.domain.model.VehicleStatus;

class FleetVehiclesTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneId.of("Europe/Lisbon"));
    private static final AcrissCode ECONOMY = new AcrissCode("ECMR");
    private static final LicensePlate FIRST = new LicensePlate("AA-12-BB");
    private static final LicensePlate SECOND = new LicensePlate("CC-34-DD");

    private final InMemoryVehicles vehicles = new InMemoryVehicles();
    private final LockedStations stations = new LockedStations();
    private final InMemoryVehicleCategories categories = new InMemoryVehicleCategories();
    private final List<StatusChange> history = new ArrayList<>();
    private final RecordedEvents events = new RecordedEvents();
    private final FleetVehicles fleet = new FleetVehicles(
            vehicles, stations, categories, history::add, events, CLOCK, mock(PlatformTransactionManager.class));

    @BeforeEach
    void setUp() {
        categories.add(VehicleCategory.create(ECONOMY, "Group B - Economy", new DriverRequirements(21, 2), null));
    }

    @Test
    void eachRegistrationPublishesTheNewCapacityOfTheStationWithTheStationLocked() {
        fleet.register(FIRST, ECONOMY, "OPO", 12000, null, "frota");
        fleet.register(SECOND, ECONOMY, "OPO", 500, null, "frota");

        assertThat(events.capacities).containsExactly(
                new StationCapacity("OPO", ECONOMY, 1), new StationCapacity("OPO", ECONOMY, 2));
        assertThat(stations.locked).containsExactly("OPO", "OPO");
        assertThat(history).extracting(StatusChange::from, StatusChange::to, StatusChange::changedBy)
                .containsOnly(tuple(null, VehicleStatus.AVAILABLE, "frota"));
    }

    @Test
    void theSameKeyGetsTheSameVehicleAndAnotherRequestIsAConflict() {
        Vehicle first = fleet.register(FIRST, ECONOMY, "OPO", 12000, "key-1", "frota");

        assertThat(fleet.register(FIRST, ECONOMY, "OPO", 12000, "key-1", "frota")).isSameAs(first);
        assertThatThrownBy(() -> fleet.register(FIRST, ECONOMY, "CMP", 0, "key-2", "frota"))
                .isInstanceOf(VehicleAlreadyRegisteredException.class);
        assertThat(events.capacities).hasSize(1);
        assertThat(history).hasSize(1);
    }

    @Test
    void aRepeatedRequestThatLosesTheRaceGetsTheVehicleOfTheFirst() {
        vehicles.registeredMeanwhile = Vehicle.register(FIRST, ECONOMY, "OPO", 12000, "key-1");

        Vehicle vehicle = fleet.register(FIRST, ECONOMY, "OPO", 12000, "key-1", "frota");

        assertThat(vehicle.registeredWith("key-1")).isTrue();
        assertThat(events.capacities).isEmpty();
    }

    @Test
    void aStationOrACategoryThatDoesNotExistIsRejected() {
        assertThatThrownBy(() -> fleet.register(FIRST, ECONOMY, "BRG", 0, null, "frota"))
                .isInstanceOf(UnknownStationException.class);
        assertThatThrownBy(() -> fleet.register(FIRST, new AcrissCode("PDAR"), "OPO", 0, null, "frota"))
                .isInstanceOf(UnknownCategoryException.class);
        assertThat(vehicles.withPlate(FIRST)).isEmpty();
        assertThat(events.capacities).isEmpty();
    }

    @Test
    void goingToMaintenanceAndBackPublishesTheCapacityAndKeepsTheReason() {
        fleet.register(FIRST, ECONOMY, "OPO", 12000, null, "frota");

        fleet.changeStatus(FIRST, VehicleStatus.IN_MAINTENANCE, "Brake service", "rui");
        fleet.changeStatus(FIRST, VehicleStatus.AVAILABLE, null, "rui");

        assertThat(events.capacities).extracting(StationCapacity::vehicles).containsExactly(1, 0, 1);
        assertThat(stations.locked).containsExactly("OPO", "OPO", "OPO");
        assertThat(history.get(1)).extracting(StatusChange::from, StatusChange::to, StatusChange::reason,
                        StatusChange::changedBy)
                .containsExactly(VehicleStatus.AVAILABLE, VehicleStatus.IN_MAINTENANCE, "Brake service", "rui");
    }

    @Test
    void retiringAVehicleInMaintenanceDoesNotChangeTheCapacity() {
        fleet.register(FIRST, ECONOMY, "OPO", 12000, null, "frota");
        fleet.changeStatus(FIRST, VehicleStatus.IN_MAINTENANCE, null, "rui");

        fleet.changeStatus(FIRST, VehicleStatus.RETIRED, "Total loss", "rui");

        assertThat(events.capacities).extracting(StationCapacity::vehicles).containsExactly(1, 0);
        assertThat(history).hasSize(3);
    }

    @Test
    void askingForTheCurrentStateWritesAndPublishesNothing() {
        fleet.register(FIRST, ECONOMY, "OPO", 12000, null, "frota");

        fleet.changeStatus(FIRST, VehicleStatus.AVAILABLE, null, "rui");

        assertThat(events.capacities).hasSize(1);
        assertThat(history).hasSize(1);
    }

    @Test
    void aRefusedTransitionChangesNothing() {
        fleet.register(FIRST, ECONOMY, "OPO", 12000, null, "frota");
        fleet.changeStatus(FIRST, VehicleStatus.RETIRED, null, "rui");

        assertThatThrownBy(() -> fleet.changeStatus(FIRST, VehicleStatus.AVAILABLE, null, "rui"))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(vehicles.withPlate(FIRST).orElseThrow().status()).isEqualTo(VehicleStatus.RETIRED);
        assertThat(history).hasSize(2);
    }

    @Test
    void changingAVehicleThatDoesNotExistIsNotFound() {
        assertThatThrownBy(() -> fleet.changeStatus(FIRST, VehicleStatus.IN_MAINTENANCE, null, "rui"))
                .isInstanceOf(VehicleNotFoundException.class);
    }
}
