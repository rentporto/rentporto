package pt.rentporto.fleet.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.application.service.FleetCatalogue;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.Transfer;
import pt.rentporto.fleet.domain.model.Vehicle;
import pt.rentporto.fleet.domain.repository.Transfers;
import pt.rentporto.fleet.domain.repository.Vehicles;
import pt.rentporto.fleet.testing.AcrissCodes;
import pt.rentporto.fleet.testing.FleetServiceTest;
import pt.rentporto.fleet.testing.LicensePlates;

/**
 * The database keeps at most one open transfer per vehicle and finds a transfer by the key of the request that
 * started it.
 */
@FleetServiceTest
class JpaTransfersTest {

    @Autowired
    private Transfers transfers;

    @Autowired
    private Vehicles vehicles;

    @Autowired
    private FleetCatalogue catalogue;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void aVehicleCannotHaveTwoOpenTransfers() {
        LicensePlate plate = registeredVehicle();
        add(transferOf(plate, null));

        assertThatThrownBy(() -> add(transferOf(plate, null))).hasStackTraceContaining("transfers_one_open_per_vehicle");
    }

    @Test
    void aTransferIsFoundByTheKeyOfTheRequestThatStartedItForThatVehicleOnly() {
        LicensePlate plate = registeredVehicle();
        Transfer transfer = transferOf(plate, "key-1");
        add(transfer);

        assertThat(transfers.startedWith(plate, "key-1")).map(Transfer::id).contains(transfer.id());
        assertThat(transfers.startedWith(plate, "key-2")).isEmpty();
        assertThat(transfers.startedWith(registeredVehicle(), "key-1")).isEmpty();
    }

    private LicensePlate registeredVehicle() {
        AcrissCode category = new AcrissCode(AcrissCodes.unique());
        catalogue.createCategory(category, "Group K - Van", new DriverRequirements(21, 2), null);
        LicensePlate plate = new LicensePlate(LicensePlates.unique());
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> vehicles.add(Vehicle.register(plate, category, "OPO", 0, null)));
        return plate;
    }

    private static Transfer transferOf(LicensePlate plate, String key) {
        return Vehicle.register(plate, new AcrissCode("ECMR"), "OPO", 0, null).startTransfer("MAT", key);
    }

    private void add(Transfer transfer) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> transfers.add(transfer));
    }
}
