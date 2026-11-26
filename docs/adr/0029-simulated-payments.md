# ADR-29: Payments with a simulated provider

- **Status:** Accepted
- **Date:** 2026-11-26
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** supersedes ADR-12; ADR-01, ADR-02, ADR-06, ADR-07, ADR-13, ADR-15, ADR-16, ADR-17, ADR-26; backlog RP-36 to RP-43, RP-53

## Context

ADR-12 put Stripe in test mode behind a `PaymentProvider` port, with a second adapter in WireMock. The system does not process real payments: what matters is to show the payment flow (the prepayment in the reservation saga, the deposit at pick-up, its settlement and the refunds) and the resilience to a slow provider, not the integration with a specific provider.

- **Cost of Stripe:** an account and test keys for each team member, the Stripe CLI running in the cluster so that the webhooks reach k3d (ADR-16), the verification of the webhook signatures and the renewal of the pre-authorisations of long rentals.
- **Reproducibility:** the demonstration scenarios (payment declined, charge above the deposit declined, slow provider) have to repeat the same way every time, without depending on an external service or on the network.
- **State of the code:** the booking-service's `payment` module only has the API contracts so far; the rental-service and the notification-service already use the payment contracts (`POST /internal/deposits`, `SettleDeposit`, `DepositSettled`, `PaymentFailed`, `PaymentRefunded`), which do not depend on the provider.

## Decision

- **A simulated provider:** the booking-service's `payment` module keeps the `PaymentProvider` port (ADR-15), with a single adapter, `SimulatedPaymentProvider`, which runs inside the service itself. There is no Stripe, no webhooks, no Stripe CLI and no WireMock as a provider.
- **Test cards:** in the reservation's `paymentMethodId`, the customer gives a test card, which decides the provider's response in every operation of that reservation (charge, deposit, charge above the deposit and refund):

  | Card | Response |
  |---|---|
  | `card_ok` | approves everything |
  | `card_declined` | declines everything (`CARD_DECLINED`) |
  | `card_no_extra` | approves the charge and the deposit; declines the charge above the deposit, which stays outstanding (`outstanding` in `DepositSettled`) |
  | `card_slow` | does not respond within the timeout (`PROVIDER_UNAVAILABLE`) |
  | any other | declines (`UNKNOWN_CARD`) |

- **Reservation charge:** the `payment` module handles `ReservationPaymentRequested` (ADR-15), calls the provider and publishes the internal event `PaymentCompleted` or `PaymentFailed`. The result comes in the provider's response, without a webhook. Each reservation is charged only once, even if the request arrives more than once.
- **Deposit:** `POST /internal/deposits` holds the deposit on the reservation's card, without debiting it. `SettleDeposit` captures the amount due, releases the rest and charges the difference to the same card; if that charge is declined, `DepositSettled` states the outstanding amount. The hold does not expire.
- **Refunds:** the provider records the full or partial refund and the module publishes `PaymentRefunded`. There are no pending charges to cancel, because the provider responds within the timeout; a payment completed after the reservation has expired is refunded.
- **Resilience:** the calls to the provider run on their own bounded thread pool (bulkhead), with a timeout and a `payment-provider` circuit breaker, created at startup so that it appears on the Grafana dashboard (ADR-26). `card_slow` makes the circuit breaker open, while search and quotes keep responding. There are no retries: a provider in the same process has no transient network failures.
- **What changes in the other ADRs** (the rest of each one stays as it is):
  - **ADR-01 and ADR-02:** the payment provider is no longer an external dependency; the bulkhead and the circuit breaker protect search and quotes from a slow simulated provider.
  - **ADR-06:** in step 2 the payment is made with the simulated provider, and in step 3 `PaymentCompleted` or `PaymentFailed` come from the provider's response, not from a webhook.
  - **ADR-07:** there are no longer Stripe webhooks to deduplicate.
  - **ADR-13:** the test payment methods become the cards above, instead of `pm_card_visa` and `pm_card_chargeDeclined`.
  - **ADR-15:** the `PaymentProvider` port isolates the simulated provider; replacing it with a real provider only touches one adapter.
  - **ADR-16:** the cluster no longer includes the Stripe CLI or WireMock.
  - **ADR-17:** there are no longer Stripe keys to protect.

## Alternatives considered

- **Stripe in test mode (ADR-12)** — shows real payments in the Stripe dashboard, but requires accounts, keys, the Stripe CLI in the cluster and the renewal of pre-authorisations, and the failure scenarios depend on Stripe and on the network.
- **WireMock as an external provider** — gives latency and failures through configuration, but it is one more service to run and configure, and the responses would have to be built by hand for each operation.
- **Always approving** — it is the simplest, but it loses the declined payments, the outstanding amount in the deposit settlement and the circuit breaker demonstration.
- **Failures and latency through configuration** — avoids the test cards, but a demonstration scenario requires changing the configuration and restarting the service, and it affects every reservation at the same time.

## Consequences

### Positive

- The payment scenarios repeat the same way every time, with no network, accounts or keys.
- Less infrastructure: no Stripe CLI, no Stripe secrets and no webhooks to expose.
- The saga, the deposit, the refunds and the circuit breaker can still be demonstrated as before.
- The contracts that the rental-service and the notification-service already use do not change.

### Negative

- No integration with a real payment provider is shown, nor any webhook verification.
- The results depend on cards agreed among ourselves, not on the behaviour of a real provider.
- The isolation is still at the thread level, not the process level: a serious failure in the JVM affects the three modules of the booking-service.
