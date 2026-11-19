package pt.rentporto.rental.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.wiremock.spring.InjectWireMock;
import pt.rentporto.events.CommandQueues;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.messaging.testing.MessagingRecords;
import pt.rentporto.rental.RentalServiceTest;
import pt.rentporto.rental.Reservations;
import pt.rentporto.rental.application.PendingAgreements;
import pt.rentporto.rental.domain.ConfirmedReservation;
import pt.rentporto.rental.domain.DiscardReason;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalStatus;
import pt.rentporto.web.testing.TestJwt;

/**
 * The agent at the counter registers the pick-up: the rental-service holds the deposit in the payments and gets a
 * vehicle from the fleet, both with the token of the agent.
 */
@RentalServiceTest
class PickUpTest {

    private static final String AGENT = TestJwt.bearer("agente", "AGENT");
    private static final String DEPOSITS = "/internal/deposits";
    private static final String ASSIGNMENTS = "/internal/vehicles/assignments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PendingAgreements pending;

    @Autowired
    private RentalAgreements agreements;

    @Autowired
    private MessagingRecords messaging;

    @Autowired
    private Resilience4JCircuitBreakerFactory circuitBreakers;

    @InjectWireMock("booking-service")
    private WireMockServer payments;

    @InjectWireMock("fleet-service")
    private WireMockServer fleet;

    @BeforeEach
    void setUp() {
        payments.resetAll();
        fleet.resetAll();
    }

    @AfterEach
    void closeTheCircuit() {
        circuitBreakers.getCircuitBreakerRegistry().circuitBreaker("payment").reset();
    }

    @Test
    void thePickUpHoldsTheDepositGetsAVehicleAndStartsTheRentalWithTheTokenOfTheAgent() throws Exception {
        RentalAgreement agreement = awaitingPickup();
        UUID deposit = depositHeld(agreement);
        vehicleAssigned(agreement, "AA-12-BB", 12000);

        pickUp(agreement.id(), """
                {"additionalDrivers": [{"fullName": "Rui Costa", "driverLicense": {"number": "P-7654321 0",
                  "category": "B", "issuingCountry": "PT", "issueDate": "2015-03-01", "expiryDate": "2035-03-01"}}],
                 "notes": "Scratch on the left door"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalId").value(agreement.id().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.licensePlate").value("AA-12-BB"))
                .andExpect(jsonPath("$.pickedUpAt").isNotEmpty())
                .andExpect(jsonPath("$.terms.deposit.amountCents").value(60000));

        payments.verify(postRequestedFor(urlEqualTo(DEPOSITS))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AGENT))
                .withHeader("Idempotency-Key", equalTo("deposit-" + agreement.id()))
                .withRequestBody(matchingJsonPath("$.reservationId", equalTo(agreement.reservationId().toString())))
                .withRequestBody(matchingJsonPath("$.amount.amountCents", equalTo("60000"))));
        fleet.verify(postRequestedFor(urlEqualTo(ASSIGNMENTS))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AGENT))
                .withHeader("Idempotency-Key", equalTo("assignment-" + agreement.id()))
                .withRequestBody(matchingJsonPath("$.categoryCode", equalTo("ECMR")))
                .withRequestBody(matchingJsonPath("$.station", equalTo("OPO"))));
        RentalAgreement active = agreements.withId(agreement.id()).orElseThrow();
        assertThat(active.depositId()).isEqualTo(deposit);
        assertThat(active.pickupOdometerKm()).isEqualTo(12000);
        assertThat(active.pickupFuelLevelEighths()).isEqualTo(8);
        String started = messaging.outbox(RoutingKeys.RENTAL_STARTED).stream()
                .filter(event -> agreement.id().toString().equals(JsonPath.read(event, "$.payload.rentalId")))
                .findFirst().orElseThrow();
        assertThat(JsonPath.<String>read(started, "$.payload.licensePlate")).isEqualTo("AA-12-BB");
        assertThat(JsonPath.<String>read(started, "$.correlationId")).isEqualTo(agreement.reservationId().toString());
    }

    @Test
    void aRefusedDepositIsA422AndNoVehicleIsAssigned() throws Exception {
        RentalAgreement agreement = awaitingPickup();
        payments.stubFor(post(DEPOSITS).willReturn(problem(422, "DEPOSIT_DECLINED")));

        pickUp(agreement.id(), "{}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DEPOSIT_DECLINED"));

        fleet.verify(0, postRequestedFor(urlEqualTo(ASSIGNMENTS)));
        assertThat(agreements.withId(agreement.id()).orElseThrow().status()).isEqualTo(RentalStatus.AWAITING_PICKUP);
    }

    @Test
    void withoutAVehicleTheDepositStaysHeldAndTheNextTryUsesIt() throws Exception {
        RentalAgreement agreement = awaitingPickup();
        UUID deposit = depositHeld(agreement);
        fleet.stubFor(post(ASSIGNMENTS).willReturn(problem(409, "NO_VEHICLE_AVAILABLE")));

        pickUp(agreement.id(), "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_VEHICLE_AVAILABLE"));
        assertThat(agreements.withId(agreement.id()).orElseThrow().depositId()).isEqualTo(deposit);

        vehicleAssigned(agreement, "CC-34-DD", 500);
        pickUp(agreement.id(), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("CC-34-DD"));
        payments.verify(1, postRequestedFor(urlEqualTo(DEPOSITS)));
    }

    @Test
    void aFleetThatFailsToAnswerIsAskedAgainWithTheSameKey() throws Exception {
        RentalAgreement agreement = awaitingPickup();
        depositHeld(agreement);
        fleet.stubFor(post(ASSIGNMENTS).inScenario("flaky").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)).willSetStateTo("up"));
        fleet.stubFor(post(ASSIGNMENTS).inScenario("flaky").whenScenarioStateIs("up")
                .willReturn(vehicle("EE-56-FF", 800)));

        pickUp(agreement.id(), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("EE-56-FF"));

        fleet.verify(2, postRequestedFor(urlEqualTo(ASSIGNMENTS))
                .withHeader("Idempotency-Key", equalTo("assignment-" + agreement.id())));
    }

    @Test
    void paymentsThatDoNotAnswerWithin5SecondsAreA503AndTheAgentCanTryAgain() throws Exception {
        RentalAgreement agreement = awaitingPickup();
        payments.stubFor(post(DEPOSITS).willReturn(deposit(UUID.randomUUID()).withFixedDelay(7000)));
        Instant start = Instant.now();

        pickUp(agreement.id(), "{}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PAYMENT_UNAVAILABLE"));

        assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(7));
        fleet.verify(0, postRequestedFor(urlEqualTo(ASSIGNMENTS)));
        payments.resetAll();
        depositHeld(agreement);
        vehicleAssigned(agreement, "AA-12-BB", 12000);
        pickUp(agreement.id(), "{}").andExpect(status().isOk());
        payments.verify(postRequestedFor(urlEqualTo(DEPOSITS))
                .withHeader("Idempotency-Key", equalTo("deposit-" + agreement.id())));
    }

    @Test
    void paymentsThatKeepFailingAreNoLongerCalledUntilTheCircuitCloses() throws Exception {
        payments.stubFor(post(DEPOSITS).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 6; i++) {
            pickUp(awaitingPickup().id(), "{}")
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("PAYMENT_UNAVAILABLE"));
        }

        payments.verify(5, postRequestedFor(urlEqualTo(DEPOSITS)));
    }

    @Test
    void refusedCardsAreAnswersThatDoNotOpenTheCircuit() throws Exception {
        payments.stubFor(post(DEPOSITS).willReturn(problem(422, "DEPOSIT_DECLINED")));
        for (int i = 0; i < 6; i++) {
            pickUp(awaitingPickup().id(), "{}").andExpect(status().isUnprocessableContent());
        }

        RentalAgreement agreement = awaitingPickup();
        depositHeld(agreement);
        vehicleAssigned(agreement, "AA-12-BB", 12000);

        pickUp(agreement.id(), "{}").andExpect(status().isOk());
    }

    @Test
    void aPickedUpOrDiscardedAgreementIsA409AndAnUnknownOneA404() throws Exception {
        RentalAgreement agreement = awaitingPickup();
        depositHeld(agreement);
        vehicleAssigned(agreement, "AA-12-BB", 12000);
        pickUp(agreement.id(), "{}").andExpect(status().isOk());

        pickUp(agreement.id(), "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_RENTAL_STATUS"));
        pickUp(UUID.randomUUID(), "{}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RENTAL_NOT_FOUND"));
        payments.verify(1, postRequestedFor(urlEqualTo(DEPOSITS)));
    }

    @Test
    void anAdditionalDriverWithAnExpiredLicenceIsA422BeforeAnyCall() throws Exception {
        RentalAgreement agreement = awaitingPickup();

        pickUp(agreement.id(), """
                {"additionalDrivers": [{"fullName": "Rui Costa", "driverLicense": {"number": "P-7654321 0",
                  "category": "B", "issuingCountry": "PT", "issueDate": "2005-03-01", "expiryDate": "2020-03-01"}}]}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DRIVER_LICENSE_NOT_VALID"));

        payments.verify(0, postRequestedFor(urlEqualTo(DEPOSITS)));
    }

    @Test
    void aDiscardedAgreementReleasesTheDepositAFailedPickUpLeftHeld() throws Exception {
        RentalAgreement agreement = awaitingPickup();
        UUID deposit = depositHeld(agreement);
        fleet.stubFor(post(ASSIGNMENTS).willReturn(problem(409, "NO_VEHICLE_AVAILABLE")));
        pickUp(agreement.id(), "{}").andExpect(status().isConflict());

        pending.reservationDiscarded(agreement.reservationId(), DiscardReason.NO_SHOW);

        String settle = messaging.outbox(CommandQueues.SETTLE_DEPOSIT).stream()
                .filter(command -> agreement.id().toString().equals(JsonPath.read(command, "$.payload.rentalId")))
                .findFirst().orElseThrow();
        assertThat(JsonPath.<String>read(settle, "$.payload.depositId")).isEqualTo(deposit.toString());
        assertThat(JsonPath.<Integer>read(settle, "$.payload.amountDue.amountCents")).isZero();
    }

    private RentalAgreement awaitingPickup() {
        ConfirmedReservation reservation = Reservations.confirmed();
        pending.reservationConfirmed(reservation);
        return agreements.forReservation(reservation.reservationId()).orElseThrow();
    }

    private UUID depositHeld(RentalAgreement agreement) {
        UUID deposit = UUID.randomUUID();
        payments.stubFor(post(DEPOSITS)
                .withRequestBody(matchingJsonPath("$.rentalId", equalTo(agreement.id().toString())))
                .willReturn(deposit(deposit)));
        return deposit;
    }

    private void vehicleAssigned(RentalAgreement agreement, String plate, long odometerKm) {
        fleet.stubFor(post(ASSIGNMENTS)
                .withRequestBody(matchingJsonPath("$.rentalId", equalTo(agreement.id().toString())))
                .willReturn(vehicle(plate, odometerKm)));
    }

    private static ResponseDefinitionBuilder deposit(UUID deposit) {
        return aResponse().withStatus(201).withHeader("Content-Type", "application/json").withBody("""
                {"depositId": "%s", "amount": {"amountCents": 60000, "currency": "EUR"},
                 "expiresAt": "2026-07-08T09:00:00Z", "status": "AUTHORIZED"}
                """.formatted(deposit));
    }

    private static ResponseDefinitionBuilder vehicle(String plate, long odometerKm) {
        return aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{\"licensePlate\": \"%s\", \"odometerKm\": %d}".formatted(plate, odometerKm));
    }

    private static ResponseDefinitionBuilder problem(int status, String code) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/problem+json")
                .withBody("{\"status\": %d, \"code\": \"%s\"}".formatted(status, code));
    }

    private ResultActions pickUp(UUID rentalId, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/rentals/{id}/pickup", rentalId)
                .header(HttpHeaders.AUTHORIZATION, AGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
