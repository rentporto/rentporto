package pt.rentporto.rental.infrastructure.client;

import feign.FeignException;
import java.util.UUID;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import pt.rentporto.events.Money;
import pt.rentporto.rental.application.DepositDeclinedException;
import pt.rentporto.rental.application.Deposits;
import pt.rentporto.rental.application.PaymentUnavailableException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pre-authorises deposits in the payment module with the token of the agent, behind a circuit breaker. The key of a
 * rental is always the same, so a pick-up tried again gets the deposit already held instead of a second one.
 */
@Component
class FeignDeposits implements Deposits {

    private static final int UNPROCESSABLE = 422;

    private final PaymentClient client;
    private final CircuitBreakerFactory<?, ?> circuitBreakers;
    private final JsonMapper jsonMapper;

    FeignDeposits(PaymentClient client, CircuitBreakerFactory<?, ?> circuitBreakers, JsonMapper jsonMapper) {
        this.client = client;
        this.circuitBreakers = circuitBreakers;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public UUID authorize(UUID rentalId, UUID reservationId, pt.rentporto.rental.domain.Money amount) {
        String authorization = CurrentAuthorization.header();
        PaymentClient.DepositRequest request =
                new PaymentClient.DepositRequest(rentalId, reservationId, Money.ofCents(amount.cents()));
        return circuitBreakers.create(ClientConfiguration.PAYMENT).run(
                () -> call(authorization, rentalId, request),
                failure -> {
                    if (failure instanceof DepositDeclinedException declined) {
                        throw declined;
                    }
                    throw new PaymentUnavailableException(rentalId);
                });
    }

    private UUID call(String authorization, UUID rentalId, PaymentClient.DepositRequest request) {
        try {
            return client.authorize(authorization, "deposit-" + rentalId, request).depositId();
        } catch (FeignException e) {
            if (e.status() == UNPROCESSABLE && "DEPOSIT_DECLINED".equals(code(e))) {
                throw new DepositDeclinedException(rentalId);
            }
            throw e;
        }
    }

    private String code(FeignException e) {
        return jsonMapper.readTree(e.contentUTF8()).path("code").asString();
    }
}
