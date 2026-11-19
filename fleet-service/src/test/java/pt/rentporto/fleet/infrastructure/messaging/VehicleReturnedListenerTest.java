package pt.rentporto.fleet.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Exchanges;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.events.rental.VehicleReturned;
import pt.rentporto.fleet.AcrissCodes;
import pt.rentporto.fleet.FleetRecords;
import pt.rentporto.fleet.FleetServiceTest;
import pt.rentporto.fleet.LicensePlates;
import pt.rentporto.fleet.application.FleetAssignments;
import pt.rentporto.fleet.application.FleetCatalogue;
import pt.rentporto.fleet.application.FleetVehicles;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.Vehicle;
import pt.rentporto.fleet.domain.VehicleStatus;
import pt.rentporto.messaging.testing.MessagingRecords;

/**
 * The rental-service publishes {@code VehicleReturned} and the fleet takes the vehicle back, once, or sends the event
 * to the dead letter queue when it does not match.
 */
@FleetServiceTest
class VehicleReturnedListenerTest {

    private static final String DEAD_LETTERS = VehicleReturnedListener.QUEUE + ".dlq";

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private MessagingRecords messaging;

    @Autowired
    private FleetRecords records;

    @Autowired
    private FleetCatalogue catalogue;

    @Autowired
    private FleetVehicles fleet;

    @Autowired
    private FleetAssignments assignments;

    @Test
    void aOneWayReturnMovesTheVehicleAndPublishesThePickUpStationBeforeTheReturnStation() {
        Rented rented = rentAt("OPO", 12000);

        UUID event = publish(rented, "MAT", 12450, 5);

        awaitHandled(event);
        assertThat(records.vehicle(rented.plate()))
                .extracting(Vehicle::status, Vehicle::station, Vehicle::odometerKm, Vehicle::fuelLevelEighths)
                .containsExactly(VehicleStatus.AVAILABLE, "MAT", 12450L, 5);
        assertThat(records.capacities(rented.category())).containsExactly("OPO=1", "OPO=0", "MAT=1");
        assertThat(records.assignment(rented.rental()).returned()).isTrue();
    }

    @Test
    void aReturnAtThePickUpStationDoesNotPublishTheCapacity() {
        Rented rented = rentAt("CMP", 3000);

        awaitHandled(publish(rented, "CMP", 3210, 8));

        assertThat(records.vehicle(rented.plate())).extracting(Vehicle::status, Vehicle::station)
                .containsExactly(VehicleStatus.AVAILABLE, "CMP");
        assertThat(records.capacities(rented.category())).containsExactly("CMP=1");
    }

    @Test
    void theSameEventTwiceOrAnotherReturnOfTheRentalIsAppliedOnce() {
        Rented rented = rentAt("OPO", 12000);
        EventEnvelope<VehicleReturned> first = envelope(rented, "MAT", 12450, 5);
        rabbit.convertAndSend(Exchanges.EVENTS, RoutingKeys.VEHICLE_RETURNED, first);
        rabbit.convertAndSend(Exchanges.EVENTS, RoutingKeys.VEHICLE_RETURNED, first);

        UUID another = publish(rented, "TRD", 13000, 2);

        awaitHandled(another);
        assertThat(records.vehicle(rented.plate())).extracting(Vehicle::station, Vehicle::odometerKm)
                .containsExactly("MAT", 12450L);
        assertThat(records.capacities(rented.category())).containsExactly("OPO=1", "OPO=0", "MAT=1");
        assertThat(records.history(rented.plate())).filteredOn(change -> change.from() == VehicleStatus.RENTED)
                .hasSize(1);
    }

    @Test
    void aReturnThatDoesNotMatchGoesToTheDeadLetterQueueAndChangesNothing() {
        Rented rented = rentAt("OPO", 12000);

        UUID event = publish(rented, "MAT", 11000, 5);

        String deadLetter = deadLetter(event);
        assertThat(JsonPath.<String>read(deadLetter, "$.payload.licensePlate")).isEqualTo(rented.plate());
        assertThat(records.vehicle(rented.plate())).extracting(Vehicle::status, Vehicle::station)
                .containsExactly(VehicleStatus.RENTED, "OPO");
        assertThat(records.capacities(rented.category())).containsExactly("OPO=1");
    }

    private Rented rentAt(String station, long odometerKm) {
        AcrissCode category = new AcrissCode(AcrissCodes.unique());
        catalogue.createCategory(category, "Group K - Van", new DriverRequirements(21, 2), null);
        LicensePlate plate = new LicensePlate(LicensePlates.unique());
        fleet.register(plate, category, station, odometerKm, null, "frota");
        UUID rental = UUID.randomUUID();
        assignments.assign(rental, category, station, "assign-" + rental, "agente");
        return new Rented(rental, plate.value(), category.value());
    }

    private UUID publish(Rented rented, String station, long odometerKm, int fuel) {
        EventEnvelope<VehicleReturned> envelope = envelope(rented, station, odometerKm, fuel);
        rabbit.convertAndSend(Exchanges.EVENTS, RoutingKeys.VEHICLE_RETURNED, envelope);
        return envelope.eventId();
    }

    private static EventEnvelope<VehicleReturned> envelope(Rented rented, String station, long odometerKm, int fuel) {
        VehicleReturned returned = new VehicleReturned(
                rented.rental(), UUID.randomUUID(), rented.plate(), station, odometerKm, fuel, List.of());
        return new EventEnvelope<>(UUID.randomUUID(), VehicleReturned.class.getSimpleName(), 1, Instant.now(),
                rented.rental().toString(), returned);
    }

    private void awaitHandled(UUID event) {
        await().atMost(Duration.ofSeconds(15)).until(() -> messaging.handled(VehicleReturnedListener.QUEUE, event));
    }

    private String deadLetter(UUID event) {
        AtomicReference<String> found = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(20)).until(() -> {
            Message message = rabbit.receive(DEAD_LETTERS, 500);
            if (message != null) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                if (event.toString().equals(JsonPath.read(body, "$.eventId"))) {
                    found.set(body);
                }
            }
            return found.get() != null;
        });
        return found.get();
    }

    private record Rented(UUID rental, String plate, String category) {
    }
}
