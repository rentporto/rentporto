package pt.rentporto.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.booking.payment.api.contract.DepositsApi;
import pt.rentporto.booking.payment.api.contract.PaymentsApi;
import pt.rentporto.booking.payment.api.contract.ReceiptsApi;
import pt.rentporto.booking.pricing.api.contract.PriceListApi;
import pt.rentporto.booking.pricing.api.contract.QuotesApi;
import pt.rentporto.booking.reservation.api.contract.AvailabilityApi;
import pt.rentporto.booking.reservation.api.contract.ReservationsApi;
import pt.rentporto.web.testing.AllowedRoles;
import pt.rentporto.web.testing.TestJwt;
import pt.rentporto.web.testing.TestJwtDecoder;

/**
 * Each endpoint of the three modules lets through the roles of the catalogue and nothing else, also when called
 * directly, without the gateway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtDecoder.class)
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuotesApi quotes;

    @Autowired
    private PriceListApi priceList;

    @Autowired
    private AvailabilityApi availability;

    @Autowired
    private ReservationsApi reservations;

    @Autowired
    private PaymentsApi payments;

    @Autowired
    private DepositsApi deposits;

    @Autowired
    private ReceiptsApi receipts;

    @Test
    void thePricingEndpointsAllowTheRolesOfTheCatalogue() {
        assertThat(AllowedRoles.of(() -> quotes.createQuote(null, null))).hasSameElementsAs(TestJwt.ROLES);
        assertThat(AllowedRoles.of(() -> priceList.rateCard(null))).containsExactly("PRICING_MANAGER");
        assertThat(AllowedRoles.of(() -> priceList.saveRateCard(null, null))).containsExactly("PRICING_MANAGER");
        assertThat(AllowedRoles.of(() -> priceList.seasons())).containsExactly("PRICING_MANAGER");
        assertThat(AllowedRoles.of(() -> priceList.replaceSeasons(null))).containsExactly("PRICING_MANAGER");
        assertThat(AllowedRoles.of(() -> priceList.fees())).containsExactly("PRICING_MANAGER");
        assertThat(AllowedRoles.of(() -> priceList.replaceFees(null))).containsExactly("PRICING_MANAGER");
        assertThat(AllowedRoles.of(() -> priceList.extras())).hasSameElementsAs(TestJwt.ROLES);
        assertThat(AllowedRoles.of(() -> priceList.replaceExtras(null))).containsExactly("PRICING_MANAGER");
        assertThat(AllowedRoles.of(() -> priceList.coverages())).hasSameElementsAs(TestJwt.ROLES);
        assertThat(AllowedRoles.of(() -> priceList.replaceCoverages(null))).containsExactly("PRICING_MANAGER");
    }

    @Test
    void theReservationEndpointsAllowTheRolesOfTheCatalogue() {
        assertThat(AllowedRoles.of(() -> availability.availability(null, null, null, null)))
                .hasSameElementsAs(TestJwt.ROLES);
        assertThat(AllowedRoles.of(() -> reservations.createReservation(null, null))).containsExactly("CUSTOMER");
        assertThat(AllowedRoles.of(() -> reservations.reservation(null)))
                .containsExactlyInAnyOrder("CUSTOMER", "AGENT");
        assertThat(AllowedRoles.of(() -> reservations.myReservations())).containsExactly("CUSTOMER");
        assertThat(AllowedRoles.of(() -> reservations.cancel(null, null)))
                .containsExactlyInAnyOrder("CUSTOMER", "AGENT");
        assertThat(AllowedRoles.of(() -> reservations.pickUpsOfTheDay(null, null))).containsExactly("AGENT");
    }

    @Test
    void thePaymentEndpointsAllowTheRolesOfTheCatalogue() {
        assertThat(AllowedRoles.of(() -> payments.payments(null)))
                .containsExactlyInAnyOrder("CUSTOMER", "AGENT", "ADMIN");
        assertThat(AllowedRoles.of(() -> deposits.authorizeDeposit(null, null))).containsExactly("AGENT");
        assertThat(AllowedRoles.of(() -> receipts.receipt(null))).containsExactly("CUSTOMER");
    }

    @Test
    void aRoleWithoutPermissionGetsA403() throws Exception {
        mockMvc.perform(get("/seasons").header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("ana", "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @MethodSource("pt.rentporto.web.testing.TestJwt#invalidTokens")
    void aMissingOrInvalidTokenGetsA401(String token) throws Exception {
        mockMvc.perform(get("/reservations/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/reservations/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void thePublicEndpointsOfTheCatalogueNeedNoToken() throws Exception {
        mockMvc.perform(get("/extras")).andExpect(status().isNotImplemented());
        mockMvc.perform(get("/coverages")).andExpect(status().isNotImplemented());
        mockMvc.perform(get("/availability")
                        .param("pickupStation", "OPO")
                        .param("returnStation", "OPO")
                        .param("from", "2026-12-01T10:00:00Z")
                        .param("to", "2026-12-05T10:00:00Z"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void theInternalEndpointNeedsTheTokenOfTheAgentAtTheCounter() throws Exception {
        mockMvc.perform(post("/internal/deposits")).andExpect(status().isUnauthorized());
    }
}
