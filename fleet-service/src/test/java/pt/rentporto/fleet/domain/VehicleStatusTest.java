package pt.rentporto.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VehicleStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "AVAILABLE, AVAILABLE, false",
            "AVAILABLE, RENTED, true",
            "AVAILABLE, IN_MAINTENANCE, true",
            "AVAILABLE, IN_TRANSFER, true",
            "AVAILABLE, RETIRED, true",
            "RENTED, AVAILABLE, true",
            "RENTED, RENTED, false",
            "RENTED, IN_MAINTENANCE, false",
            "RENTED, IN_TRANSFER, false",
            "RENTED, RETIRED, false",
            "IN_MAINTENANCE, AVAILABLE, true",
            "IN_MAINTENANCE, RENTED, false",
            "IN_MAINTENANCE, IN_MAINTENANCE, false",
            "IN_MAINTENANCE, IN_TRANSFER, false",
            "IN_MAINTENANCE, RETIRED, true",
            "IN_TRANSFER, AVAILABLE, true",
            "IN_TRANSFER, RENTED, false",
            "IN_TRANSFER, IN_MAINTENANCE, false",
            "IN_TRANSFER, IN_TRANSFER, false",
            "IN_TRANSFER, RETIRED, false",
            "RETIRED, AVAILABLE, false",
            "RETIRED, RENTED, false",
            "RETIRED, IN_MAINTENANCE, false",
            "RETIRED, IN_TRANSFER, false",
            "RETIRED, RETIRED, false"})
    void eachTransitionIsAllowedOrNot(VehicleStatus from, VehicleStatus to, boolean allowed) {
        assertThat(from.canChangeTo(to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @CsvSource({
            "AVAILABLE, true",
            "RENTED, true",
            "IN_MAINTENANCE, false",
            "IN_TRANSFER, false",
            "RETIRED, false"})
    void availableAndRentedVehiclesCountInTheCapacityOfTheirStation(VehicleStatus status, boolean counts) {
        assertThat(VehicleStatus.countedInCapacity().contains(status)).isEqualTo(counts);
    }
}
