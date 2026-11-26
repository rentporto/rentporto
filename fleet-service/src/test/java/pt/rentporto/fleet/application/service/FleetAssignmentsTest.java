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
import pt.rentporto.fleet.domain.exception.NoVehicleAvailableException;
import pt.rentporto.fleet.domain.exception.RentalAlreadyAssignedException;
import pt.rentporto.fleet.domain.exception.UnknownCategoryException;
import pt.rentporto.fleet.domain.exception.UnknownStationException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.StatusChange;
import pt.rentporto.fleet.domain.model.Vehicle;
import pt.rentporto.fleet.domain.model.VehicleAssignment;
import pt.rentporto.fleet.domain.model.VehicleCategory;
import pt.rentporto.fleet.domain.model.VehicleStatus;

class FleetAssignmentsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneId.of("Europe/Lisbon"));
    private static final AcrissCode ECONOMY = new AcrissCode("ECMR");
    private static final AcrissCode PREMIUM = new AcrissCode("PDAR");
    private static final LicensePlate AT_PORTO = new LicensePlate("AA-12-BB");
    private static final LicensePlate PREMIUM_AT_PORTO = new LicensePlate("CC-34-DD");
    private static final LicensePlate AT_CAMPANHA = new LicensePlate("EE-56-FF");
    private static final UUID RENTAL = UUID.fromString("5c4b3a29-1807-4f6e-9d5c-4b3a29180706");

    private final InMemoryVehicles vehicles = new InMemoryVehicles();
    private final InMemoryAssignments assignments = new InMemoryAssignments();
    private final InMemoryVehicleCategories categories = new InMemoryVehicleCategories();
    private final List<StatusChange> history = new ArrayList<>();
    private final FleetAssignments fleet = new FleetAssignments(
            vehicles, assignments, new LockedStations(), categories, history::add, CLOCK,
            mock(PlatformTransactionManager.class));

    @BeforeEach
    void setUp() {
        categories.add(VehicleCategory.create(ECONOMY, "Group B - Economy", new DriverRequirements(21, 2), null));
        categories.add(VehicleCategory.create(PREMIUM, "Group E - Premium", new DriverRequirements(25, 2), null));
        vehicles.add(Vehicle.register(AT_PORTO, ECONOMY, "OPO", 12000, null));
        vehicles.add(Vehicle.register(PREMIUM_AT_PORTO, PREMIUM, "OPO", 500, null));
        vehicles.add(Vehicle.register(AT_CAMPANHA, ECONOMY, "CMP", 800, null));
    }

    @Test
    void theRentalGetsAnAvailableVehicleOfTheCategoryAtTheStationWhichBecomesRented() {
        VehicleAssignment assignment = fleet.assign(RENTAL, ECONOMY, "OPO", "key-1", "agente");

        assertThat(assignment.plate()).isEqualTo(AT_PORTO);
        assertThat(assignment.odometerKm()).isEqualTo(12000);
        assertThat(vehicles.withPlate(AT_PORTO).orElseThrow().status()).isEqualTo(VehicleStatus.RENTED);
        assertThat(assignments.forRental(RENTAL)).contains(assignment);
        assertThat(history).singleElement()
                .extracting(StatusChange::from, StatusChange::to, StatusChange::reason, StatusChange::changedBy)
                .containsExactly(VehicleStatus.AVAILABLE, VehicleStatus.RENTED, "Rental " + RENTAL, "agente");
    }

    @Test
    void theSameKeyGetsTheSameAssignmentAndAnotherKeyIsAConflict() {
        VehicleAssignment assignment = fleet.assign(RENTAL, ECONOMY, "OPO", "key-1", "agente");

        assertThat(fleet.assign(RENTAL, ECONOMY, "OPO", "key-1", "agente")).isSameAs(assignment);
        assertThatThrownBy(() -> fleet.assign(RENTAL, ECONOMY, "OPO", "key-2", "agente"))
                .isInstanceOf(RentalAlreadyAssignedException.class);
        assertThat(history).hasSize(1);
    }

    @Test
    void theRentalIsLockedBeforeItsAssignmentIsLookedFor() {
        fleet.assign(RENTAL, ECONOMY, "OPO", "key-1", "agente");
        fleet.assign(RENTAL, ECONOMY, "OPO", "key-1", "agente");

        assertThat(assignments.lockedBeforeLookingFor).containsExactly(RENTAL, RENTAL);
    }

    @Test
    void aStationWithoutAnAvailableVehicleOfTheCategoryIsAConflict() {
        fleet.assign(RENTAL, ECONOMY, "OPO", "key-1", "agente");

        assertThatThrownBy(() -> fleet.assign(UUID.randomUUID(), ECONOMY, "OPO", "key-2", "agente"))
                .isInstanceOf(NoVehicleAvailableException.class);
        assertThatThrownBy(() -> fleet.assign(UUID.randomUUID(), PREMIUM, "CMP", "key-3", "agente"))
                .isInstanceOf(NoVehicleAvailableException.class);
    }

    @Test
    void aStationOrACategoryThatDoesNotExistIsRejected() {
        assertThatThrownBy(() -> fleet.assign(RENTAL, ECONOMY, "BRG", "key-1", "agente"))
                .isInstanceOf(UnknownStationException.class);
        assertThatThrownBy(() -> fleet.assign(RENTAL, new AcrissCode("CDMR"), "OPO", "key-1", "agente"))
                .isInstanceOf(UnknownCategoryException.class);
        assertThat(assignments.forRental(RENTAL)).isEmpty();
    }
}
