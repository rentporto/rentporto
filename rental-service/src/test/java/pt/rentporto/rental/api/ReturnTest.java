package pt.rentporto.rental.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.events.CommandQueues;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.messaging.testing.MessagingRecords;
import pt.rentporto.rental.RentalServiceTest;
import pt.rentporto.rental.Reservations;
import pt.rentporto.rental.application.PendingAgreements;
import pt.rentporto.rental.domain.ConfirmedReservation;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalStatus;
import pt.rentporto.web.testing.TestJwt;

/**
 * The agent at the counter registers the return: the agreement gets its extra charges, the fleet hears of the
 * vehicle and the payments are asked to settle the deposit.
 */
@RentalServiceTest
class ReturnTest {

    private static final String AGENT = TestJwt.bearer("agente", "AGENT");
    private static final String FULL_TANK = """
            {"station": "MAT", "inspection": {"odometerKm": 12450, "fuelLevelEighths": 8, "damages": []}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PendingAgreements pending;

    @Autowired
    private RentalAgreements agreements;

    @Autowired
    private MessagingRecords messaging;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Clock clock;

    @Test
    void aReturnOnTimeChargesTheFuelAndTheDamagesTellsTheFleetAndSettlesTheDeposit() throws Exception {
        RentalAgreement agreement = pickedUp(clock.instant().plusSeconds(3600));

        returnVehicle(agreement.id(), """
                {"station": "MAT", "litresToFill": 18.5,
                 "inspection": {"odometerKm": 12450, "fuelLevelEighths": 3, "notes": "Dirty",
                  "damages": [{"location": "FRONT_BUMPER", "description": "10 cm scratch",
                               "amount": {"amountCents": 20000, "currency": "EUR"}}]}}
                """)
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/rentals/" + agreement.id())))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty())
                .andExpect(jsonPath("$.extraCharges[0].type").value("FUEL"))
                .andExpect(jsonPath("$.extraCharges[0].description").value("18.5 litres to fill"))
                .andExpect(jsonPath("$.extraCharges[0].amount.amountCents").value(6478))
                .andExpect(jsonPath("$.extraCharges[1].type").value("DAMAGE"))
                .andExpect(jsonPath("$.extraCharges[1].amount.amountCents").value(20000))
                .andExpect(jsonPath("$.extraCharges.length()").value(2));

        String returned = sent(RoutingKeys.VEHICLE_RETURNED, agreement).orElseThrow();
        assertThat(JsonPath.<String>read(returned, "$.payload.licensePlate")).isEqualTo("AA-12-BB");
        assertThat(JsonPath.<String>read(returned, "$.payload.returnStation")).isEqualTo("MAT");
        assertThat(JsonPath.<Integer>read(returned, "$.payload.odometerKm")).isEqualTo(12450);
        assertThat(JsonPath.<Integer>read(returned, "$.payload.fuelLevelEighths")).isEqualTo(8);
        assertThat(agreements.withId(agreement.id()).orElseThrow().returnFuelLevelEighths()).isEqualTo(3);
        assertThat(JsonPath.<List<String>>read(returned, "$.payload.damages[*].location"))
                .containsExactly("FRONT_BUMPER");
        String settle = sent(CommandQueues.SETTLE_DEPOSIT, agreement).orElseThrow();
        assertThat(JsonPath.<String>read(settle, "$.payload.depositId")).isEqualTo(agreement.depositId().toString());
        assertThat(JsonPath.<Integer>read(settle, "$.payload.amountDue.amountCents")).isEqualTo(26478);
        assertThat(agreements.withId(agreement.id()).orElseThrow().status()).isEqualTo(RentalStatus.RETURNED);
    }

    @Test
    void aReturnTwoHoursLateChargesOneExtraDay() throws Exception {
        RentalAgreement agreement = pickedUp(clock.instant().minusSeconds(2 * 3600));

        returnVehicle(agreement.id(), FULL_TANK)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.extraCharges[0].type").value("LATE_RETURN"))
                .andExpect(jsonPath("$.extraCharges[0].description").value("1 extra day"))
                .andExpect(jsonPath("$.extraCharges[0].amount.amountCents").value(6150));

        assertThat(JsonPath.<Integer>read(sent(CommandQueues.SETTLE_DEPOSIT, agreement).orElseThrow(),
                "$.payload.amountDue.amountCents")).isEqualTo(6150);
    }

    @Test
    void aReturnThatDoesNotMatchIsA422AndPublishesNothing() throws Exception {
        RentalAgreement agreement = pickedUp(clock.instant().plusSeconds(3600));

        returnVehicle(agreement.id(), FULL_TANK.replace("MAT", "OPO"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("WRONG_RETURN_STATION"));
        returnVehicle(agreement.id(), FULL_TANK.replace("12450", "11999"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ODOMETER_BELOW_PICKUP"));
        returnVehicle(agreement.id(), FULL_TANK.replace("\"fuelLevelEighths\": 8", "\"fuelLevelEighths\": 5"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("FUEL_MISMATCH"));

        assertThat(sent(RoutingKeys.VEHICLE_RETURNED, agreement)).isEmpty();
        assertThat(sent(CommandQueues.SETTLE_DEPOSIT, agreement)).isEmpty();
        assertThat(agreements.withId(agreement.id()).orElseThrow().status()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void onlyAnActiveAgreementIsReturnedAndAnUnknownOneIsNotFound() throws Exception {
        ConfirmedReservation reservation = Reservations.returningAt(clock.instant().plusSeconds(3600));
        pending.reservationConfirmed(reservation);
        UUID waiting = agreements.forReservation(reservation.reservationId()).orElseThrow().id();
        RentalAgreement agreement = pickedUp(clock.instant().plusSeconds(3600));
        returnVehicle(agreement.id(), FULL_TANK).andExpect(status().isAccepted());

        returnVehicle(waiting, FULL_TANK)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RENTAL_STATUS"));
        returnVehicle(agreement.id(), FULL_TANK)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RENTAL_STATUS"));
        returnVehicle(UUID.randomUUID(), FULL_TANK)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RENTAL_NOT_FOUND"));
    }

    @Test
    void litresWithMoreThanTwoDecimalsAreAnInvalidRequest() throws Exception {
        RentalAgreement agreement = pickedUp(clock.instant().plusSeconds(3600));

        returnVehicle(agreement.id(), FULL_TANK.replace("\"station\"", "\"litresToFill\": 1.555, \"station\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private RentalAgreement pickedUp(Instant returnAt) {
        ConfirmedReservation reservation = Reservations.returningAt(returnAt);
        pending.reservationConfirmed(reservation);
        return new TransactionTemplate(transactionManager).execute(status -> {
            RentalAgreement agreement = agreements.forReservation(reservation.reservationId()).orElseThrow();
            agreement.recordDeposit(UUID.randomUUID());
            agreement.pickUp("AA-12-BB", 12000, List.of(), null, clock.instant(), LocalDate.now(clock));
            agreements.update(agreement);
            return agreement;
        });
    }

    private Optional<String> sent(String routingKey, RentalAgreement agreement) {
        return messaging.outbox(routingKey).stream()
                .filter(message -> agreement.id().toString().equals(JsonPath.read(message, "$.payload.rentalId")))
                .findFirst();
    }

    private ResultActions returnVehicle(UUID rentalId, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/rentals/{id}/return", rentalId)
                .header(HttpHeaders.AUTHORIZATION, AGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
