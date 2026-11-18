package pt.rentporto.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import pt.rentporto.fleet.AcrissCodes;
import pt.rentporto.fleet.FleetServiceTest;
import pt.rentporto.fleet.LicensePlates;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.InvalidStatusTransitionException;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.VehicleStatus;

/**
 * Changes at the same time happen one after the other: the capacity the booking-service reads last is the real one,
 * a vehicle changes state once and starts one transfer.
 */
@FleetServiceTest
class ConcurrentChangesTest {

    private static final int REQUESTS = 8;

    @Autowired
    private FleetVehicles fleet;

    @Autowired
    private FleetTransfers transfers;

    @Autowired
    private FleetCatalogue catalogue;

    @Autowired
    private JdbcTemplate jdbc;

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
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM vehicle_status_changes WHERE license_plate = ? AND to_status = 'IN_MAINTENANCE'",
                Integer.class, plate.value())).isEqualTo(1);
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
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM transfers WHERE license_plate = ?", Integer.class, plate.value())).isEqualTo(1);
    }

    private AcrissCode newCategory() {
        AcrissCode category = new AcrissCode(AcrissCodes.unique());
        catalogue.createCategory(category, "Group K - Van", new DriverRequirements(21, 2), null);
        return category;
    }

    private static void runTogether(IntFunction<Callable<?>> request) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> requests = new ArrayList<>();
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
                future.get();
            }
        }
    }

    private List<Integer> capacities(AcrissCode category) {
        return jdbc.queryForList(
                "SELECT (payload -> 'payload' ->> 'capacity')::int FROM outbox"
                        + " WHERE routing_key = 'fleet.station-capacity-changed'"
                        + " AND payload -> 'payload' ->> 'stationCode' = 'TRD'"
                        + " AND payload -> 'payload' ->> 'categoryCode' = ? ORDER BY id",
                Integer.class, category.value());
    }
}
