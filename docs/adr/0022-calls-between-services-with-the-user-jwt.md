# ADR-22: Calls between services with the user's JWT

- **Status:** Accepted
- **Date:** 2026-11-17
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-02, ADR-08, ADR-21; backlog RP-11, RP-27, RP-45

## Context

Each service validates the JWT and the roles, even on requests that do not go through the gateway (ADR-08). The `/internal/**` endpoints are not exposed by the gateway (ADR-21), but they are reachable inside the cluster and need protection. There are three:

| Endpoint | Who calls | Who is waiting |
|---|---|---|
| `GET /internal/customers/{id}/driver-profile` | booking-service, when creating the reservation | the customer |
| `POST /internal/vehicles/assignments` | rental-service, at pick-up | the agent |
| `POST /internal/deposits` | rental-service, at pick-up | the agent |

Synchronous REST is only used when someone is waiting for the response (ADR-02). Everything else goes through messages, so there is always a user behind an internal call.

## Decision

- **The caller forwards the token:** the calling service forwards the `Authorization` header of the request it received, with a Feign interceptor.
- **The internal endpoint is treated like the others:** it validates the JWT and requires the role of whoever is waiting, with `@PreAuthorize`:
  - CUSTOMER on the driver profile;
  - AGENT on the vehicle assignment and on the deposit.
- **No credentials of their own:** there are no service tokens and no secrets shared between services.

## Alternatives considered

- **Client credentials in the auth-service** — each service would have its own token, but that requires an endpoint, a secret per service and token renewal, and it is only needed for calls without a user, which ADR-02 does not foresee.
- **A shared API key** — it is simple, but a single secret opens every internal endpoint, and the called service no longer knows who originated the request.
- **Trusting only the cluster network** — any compromised pod could call the internal endpoints without restriction.

## Consequences

### Positive

- An internal endpoint only does what the user behind the request is allowed to do.
- The called service knows who originated the request, which gives traceability and lets it check whether the resource belongs to the user.
- There are no service credentials to manage.

### Negative

- A token about to expire can expire in the middle of a chain of calls; the client repeats the request after logging in again.
- If one day an internal call happens without a user (for example in a scheduled job), this decision has to be revisited.
