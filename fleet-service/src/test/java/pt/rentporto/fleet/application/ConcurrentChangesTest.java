package pt.rentporto.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pt.rentporto.fleet.AcrissCodes;
import pt.rentporto.fleet.FleetRecords;
import pt.rentporto.fleet.FleetServiceTest;
import pt.rentporto.fleet.LicensePlates;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.InvalidStatusTransitionException;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.NoVehicleAvailableException;
import pt.rentporto.fleet.domain.VehicleStatus;

/**
 * Changes at the same time happen one after the other: the capacity the booking-service reads last is the real one,
 * a vehicle changes state once and starts one transfer, and two rentals never get the same vehicle.
 */
@FleetServiceTest
class ConcurrentChangesTest {

    private static final int REQUESTS = 8;

    @Autowired
    private FleetVehicles fleet;

    @Autowired
    private FleetTransfers transfers;

    @Autowired
    private FleetAssignments assignments;

    @Autowired
    private FleetCatalogue catalogue;

    @Autowired
    private FleetRecords records;

    @Test
    void simultaneousRegistrationsAtAStationPublishEachCapacityOnceInOrder() throws Exception {
        AcrissCode category = newCategory();

        runTogether(i -> {
            LicensePlate plate = new LicensePlate(LicensePlates.unique());
            return () -> fleet.register(plate, category, "TRD", 0, null, "frota");
        });

        assertThat(capacities(category)).isEqualTo(IntStream.rangeClosed(1, REQUESTS).boxed().toList());
    }

    @Test
    void simultaneousChangesOfAVehicleChangeItOnce() throws Exception {
        AcrissCode category = newCategory();
        LicensePlate plate = new LicensePlate(LicensePlates.unique());
        fleet.register(plate, category, "TRD", 0, null, "frota");

        runTogether(i -> () -> fleet.changeStatus(plate, VehicleStatus.IN_MAINTENANCE, "Request " + i, "frota"));

        assertThat(capacities(category)).containsExactly(1, 0);
        assertThat(records.history(plate.value())).filteredOn(change -> change.to() == VehicleStatus.IN_MAINTENANCE)
                .hasSize(1);
    }

    @Test
    void simultaneousTransfersOfAVehicleStartOnlyOne() throws Exception {
        AcrissCode category = newCategory();
        LicensePlate plate = new LicensePlate(LicensePlates.unique());
        fleet.register(plate, category, "TRD", 0, null, "frota");

        runTogether(i -> () -> {
            try {
                return transfers.start(plate, i % 2 == 0 ? "MAT" : "OPO", null, "frota");
            } catch (InvalidStatusTransitionException e) {
                return null;
            }
        });

        assertThat(capacities(category)).containsExactly(1, 0);
        assertThat(records.transfers(plate.value())).isEqualTo(1);
    }

    @Test
    void simultaneousAssignmentsNeverGetTheSameVehicle() throws Exception {
        AcrissCode category = newCategory();
        for (int i = 0; i < 5; i++) {
            fleet.register(new LicensePlate(LicensePlates.unique()), category, "TRD", 0, null, "frota");
        }

        List<Object> results = runTogether(i -> () -> {
            try {
                return assignments.assign(UUID.randomUUID(), category, "TRD", "key-" + i, "agente").plate();
            } catch (NoVehicleAvailableException e) {
                return e.code();
            }
        });

        assertThat(results.stream().filter(LicensePlate.class::isInstance).distinct()).hasSize(5);
        assertThat(results).filteredOn("NO_VEHICLE_AVAILABLE"::equals).hasSize(REQUESTS - 5);
    }

    @Test
    void simultaneousAssignmentsOfARentalAssignOneVehicle() throws Exception {
        AcrissCode category = newCategory();
        for (int i = 0; i < 3; i++) {
            fleet.register(new LicensePlate(LicensePlates.unique()), category, "TRD", 0, null, "frota");
        }
        UUID rental = UUID.randomUUID();

        List<Object> results = runTogether(
                i -> () -> assignments.assign(rental, category, "TRD", "key-" + rental, "agente").plate());

        assertThat(results.stream().distinct()).hasSize(1);
        assertThat(records.vehicles(category.value(), VehicleStatus.RENTED)).isEqualTo(1);
    }

    private AcrissCode newCategory() {
        AcrissCode category = new AcrissCode(AcrissCodes.unique());
        catalogue.createCategory(category, "Group K - Van", new DriverRequirements(21, 2), null);
        return category;
    }

    private static List<Object> runTogether(IntFunction<Callable<?>> request) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> requests = new ArrayList<>();
        List<Object> results = new ArrayList<>();
        try (ExecutorService threads = Executors.newFixedThreadPool(REQUESTS)) {
            for (int i = 0; i < REQUESTS; i++) {
                Callable<?> call = request.apply(i);
                requests.add(threads.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            start.countDown();
            for (Future<?> future : requests) {
                results.add(future.get());
            }
        }
        return results;
    }

    private List<Integer> capacities(AcrissCode category) {
        return records.capacities("TRD", category.value());
    }
}
