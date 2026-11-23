package pt.rentporto.rental.infrastructure.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalStatus;

/**
 * The agreements in each state, for the RentPorto dashboard of Grafana (ADR-26). Each gauge counts in the database when
 * Prometheus reads it, so every replica reports the total of the service.
 */
@Component
class AgreementMetrics implements MeterBinder {

    static final String AGREEMENTS = "rentporto.rental.agreements";

    private final RentalAgreements agreements;

    AgreementMetrics(RentalAgreements agreements) {
        this.agreements = agreements;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (RentalStatus status : RentalStatus.values()) {
            Gauge.builder(AGREEMENTS, agreements, stored -> stored.countIn(status))
                    .description("Rental agreements in each state")
                    .tag("status", status.name())
                    .register(registry);
        }
    }
}
