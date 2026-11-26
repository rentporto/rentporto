package pt.rentporto.fleet.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.application.service.FleetCatalogue;
import pt.rentporto.fleet.domain.exception.RentalAlreadyAssignedException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.Vehicle;
import pt.rentporto.fleet.domain.repository.VehicleAssignments;
import pt.rentporto.fleet.domain.repository.Vehicles;
import pt.rentporto.fleet.testing.AcrissCodes;
import pt.rentporto.fleet.testing.FleetServiceTest;
import pt.rentporto.fleet.testing.LicensePlates;

/**
 * The rental is the primary key of the assignments, and a second one becomes a domain exception.
 */
@FleetServiceTest
class JpaVehicleAssignmentsTest {

    @Autowired
    private VehicleAssignments assignments;

    @Autowired
    private Vehicles vehicles;

    @Autowired
    private FleetCatalogue catalogue;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void assigningARentalTwiceTellsTheSecondRequest() {
        UUID rental = UUID.randomUUID();
        assign(rental);

        assertThatThrownBy(() -> assign(rental)).isInstanceOf(RentalAlreadyAssignedException.class);
    }

    private void assign(UUID rental) {
        AcrissCode category = new AcrissCode(AcrissCodes.unique());
        catalogue.createCategory(category, "Group K - Van", new DriverRequirements(21, 2), null);
        Vehicle vehicle = Vehicle.register(new LicensePlate(LicensePlates.unique()), category, "OPO", 0, null);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            vehicles.add(vehicle);
            assignments.add(vehicle.assignTo(rental, "key-" + rental));
        });
    }
}
