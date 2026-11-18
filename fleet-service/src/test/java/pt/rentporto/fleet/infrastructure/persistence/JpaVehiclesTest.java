package pt.rentporto.fleet.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.AcrissCodes;
import pt.rentporto.fleet.FleetServiceTest;
import pt.rentporto.fleet.LicensePlates;
import pt.rentporto.fleet.application.FleetCatalogue;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.Vehicle;
import pt.rentporto.fleet.domain.VehicleAlreadyRegisteredException;
import pt.rentporto.fleet.domain.VehicleStatus;
import pt.rentporto.fleet.domain.Vehicles;

/**
 * The capacity counts the available and the rented vehicles of a category at a station, and the primary key of the
 * table becomes a domain exception.
 */
@FleetServiceTest
class JpaVehiclesTest {

    @Autowired
    private Vehicles vehicles;

    @Autowired
    private FleetCatalogue catalogue;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theCapacityCountsTheAvailableAndTheRentedVehiclesOfTheCategoryAtTheStation() {
        AcrissCode category = newCategory();
        for (VehicleStatus status : VehicleStatus.values()) {
            LicensePlate plate = add(category, "OPO");
            jdbc.update("UPDATE vehicles SET status = ? WHERE license_plate = ?", status.name(), plate.value());
        }
        add(category, "CMP");
        add(newCategory(), "OPO");

        assertThat(vehicles.capacity("OPO", category).vehicles()).isEqualTo(2);
        assertThat(vehicles.capacity("CMP", category).vehicles()).isEqualTo(1);
        assertThat(vehicles.capacity("TRD", category).vehicles()).isZero();
    }

    @Test
    void storingTheSamePlateTwiceTellsTheSecondRequest() {
        AcrissCode category = newCategory();
        LicensePlate plate = add(category, "OPO");

        assertThatThrownBy(() -> add(plate, category, "OPO")).isInstanceOf(VehicleAlreadyRegisteredException.class);
    }

    private AcrissCode newCategory() {
        AcrissCode code = new AcrissCode(AcrissCodes.unique());
        catalogue.createCategory(code, "Group K - Van", new DriverRequirements(21, 2), null);
        return code;
    }

    private LicensePlate add(AcrissCode category, String station) {
        LicensePlate plate = new LicensePlate(LicensePlates.unique());
        add(plate, category, station);
        return plate;
    }

    private void add(LicensePlate plate, AcrissCode category, String station) {
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> vehicles.add(Vehicle.register(plate, category, station, 0, null)));
    }
}
