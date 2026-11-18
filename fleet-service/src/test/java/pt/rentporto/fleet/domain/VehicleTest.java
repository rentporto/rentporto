package pt.rentporto.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VehicleTest {

    private final Vehicle vehicle =
            Vehicle.register(new LicensePlate("AA-12-BB"), new AcrissCode("ECMR"), "OPO", 12000, "key-1");

    @Test
    void aRegisteredVehicleIsAvailableAtItsStationWithAFullTank() {
        assertThat(vehicle.status()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.station()).isEqualTo("OPO");
        assertThat(vehicle.odometerKm()).isEqualTo(12000);
        assertThat(vehicle.fuelLevelEighths()).isEqualTo(8);
        assertThat(vehicle.countsInCapacity()).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "AVAILABLE, IN_MAINTENANCE",
            "IN_MAINTENANCE, AVAILABLE",
            "AVAILABLE, RETIRED",
            "IN_MAINTENANCE, RETIRED"})
    void aFleetManagerSendsItToMaintenanceAndBackOrRetiresIt(VehicleStatus from, VehicleStatus to) {
        moveTo(from);

        assertThat(vehicle.changeStatus(to)).isTrue();
        assertThat(vehicle.status()).isEqualTo(to);
    }

    @ParameterizedTest(name = "RETIRED -> {0}")
    @CsvSource({"AVAILABLE", "IN_MAINTENANCE"})
    void aRetiredVehicleNeverComesBack(VehicleStatus to) {
        vehicle.changeStatus(VehicleStatus.RETIRED);

        assertThatThrownBy(() -> vehicle.changeStatus(to))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat(vehicle.status()).isEqualTo(VehicleStatus.RETIRED);
    }

    @ParameterizedTest
    @CsvSource({"RENTED", "IN_TRANSFER"})
    void aFleetManagerCannotRentOrTransferIt(VehicleStatus to) {
        assertThatThrownBy(() -> vehicle.changeStatus(to)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(vehicle.status()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void askingForTheCurrentStateChangesNothing() {
        assertThat(vehicle.changeStatus(VehicleStatus.AVAILABLE)).isFalse();
        vehicle.changeStatus(VehicleStatus.RETIRED);
        assertThat(vehicle.changeStatus(VehicleStatus.RETIRED)).isFalse();
    }

    @Test
    void aVehicleInMaintenanceOrRetiredDoesNotCountInTheCapacity() {
        vehicle.changeStatus(VehicleStatus.IN_MAINTENANCE);
        assertThat(vehicle.countsInCapacity()).isFalse();
        vehicle.changeStatus(VehicleStatus.RETIRED);
        assertThat(vehicle.countsInCapacity()).isFalse();
    }

    @Test
    void onlyTheKeyOfTheRequestThatRegisteredItRepeatsIt() {
        assertThat(vehicle.registeredWith("key-1")).isTrue();
        assertThat(vehicle.registeredWith("key-2")).isFalse();
        assertThat(vehicle.registeredWith(null)).isFalse();
    }

    @Test
    void theOdometerCannotBeNegative() {
        LicensePlate plate = new LicensePlate("AA-12-BB");
        AcrissCode economy = new AcrissCode("ECMR");

        assertThatThrownBy(() -> Vehicle.register(plate, economy, "OPO", -1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void moveTo(VehicleStatus status) {
        if (status != VehicleStatus.AVAILABLE) {
            vehicle.changeStatus(status);
        }
    }
}
