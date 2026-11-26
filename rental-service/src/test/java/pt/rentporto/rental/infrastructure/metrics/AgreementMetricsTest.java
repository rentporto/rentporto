package pt.rentporto.rental.infrastructure.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pt.rentporto.rental.domain.model.RentalStatus;
import pt.rentporto.rental.testing.RentalServiceTest;
import pt.rentporto.rental.testing.TestAgreements;

/**
 * The metrics of the RentPorto dashboard: the agreements in each state, and the circuit breaker of the payments from
 * the start of the service.
 */
@RentalServiceTest
class AgreementMetricsTest {

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private TestAgreements testAgreements;

    @Test
    void eachStateCountsItsAgreements() {
        Map<RentalStatus, Double> before = agreementsByStatus();

        testAgreements.awaitingPickup();
        testAgreements.active();
        testAgreements.active();
        testAgreements.returned();

        Map<RentalStatus, Double> after = agreementsByStatus();
        assertThat(after.get(RentalStatus.AWAITING_PICKUP) - before.get(RentalStatus.AWAITING_PICKUP)).isEqualTo(1);
        assertThat(after.get(RentalStatus.ACTIVE) - before.get(RentalStatus.ACTIVE)).isEqualTo(2);
        assertThat(after.get(RentalStatus.RETURNED) - before.get(RentalStatus.RETURNED)).isEqualTo(1);
        assertThat(after.get(RentalStatus.CLOSED)).isEqualTo(before.get(RentalStatus.CLOSED));
        assertThat(after.get(RentalStatus.DISCARDED)).isEqualTo(before.get(RentalStatus.DISCARDED));
    }

    @Test
    void theCircuitBreakerOfThePaymentsIsInTheMetricsBeforeItsFirstCall() {
        assertThat(registry.find("resilience4j.circuitbreaker.state").tag("name", "payment").gauges())
                .extracting(Gauge::getId)
                .extracting(id -> id.getTag("state"))
                .contains("closed", "open", "half_open");
    }

    private Map<RentalStatus, Double> agreementsByStatus() {
        Map<RentalStatus, Double> counts = new EnumMap<>(RentalStatus.class);
        for (RentalStatus status : RentalStatus.values()) {
            counts.put(status, registry.get(AgreementMetrics.AGREEMENTS).tag("status", status.name()).gauge().value());
        }
        return counts;
    }
}
