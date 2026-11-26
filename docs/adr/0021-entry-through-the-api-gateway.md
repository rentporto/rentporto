# ADR-21: Entry through the API Gateway

- **Status:** Accepted
- **Date:** 2026-11-17
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-04, ADR-08, ADR-13, ADR-16, ADR-19, ADR-20; backlog RP-10, RP-11

## Context

All external traffic enters through the gateway, the only service the Ingress exposes. The gateway routes to six services. Clients must not know that topology: if a resource moves to another service, the URL must not change. Security has to be applied at the entry without duplicating each service's role rules, and abuse has to be stopped, such as attempts to guess passwords. There is no frontend (ADR-13): the API is used through the Swagger UI and the Bruno collection.

## Decision

- **Routes by resource.**
  - The public API lives at `/api/v1/{resource}`. The gateway removes `/api/v1` and routes to the service that owns the resource (e.g. `/api/v1/reservations/...` → booking-service `/reservations/...`).
  - The route table is gateway code. The service addresses come from the Config Server by profile (ADR-19), with the same ports in `dev` and in `k8s`.
- **Internal endpoints:** `/internal/**` has no route, so from outside the cluster it answers 404.
- **Authentication in the gateway, authorisation in the services.**
  - The gateway validates the JWT's signature with the key from `/auth/jwks` (ADR-20), its expiry and its `iss`, and forwards the `Authorization` header.
  - Roles are checked with `@PreAuthorize` in each service, which validates the token again (ADR-08).
  - The public catalogue routes pass without a token and ignore an old token.
- **Hidden topology.**
  - The gateway sends `X-Forwarded-Host`, `X-Forwarded-Proto` and `X-Forwarded-Prefix`.
  - With `server.forward-headers-strategy: framework`, the services build the links and the `Location` with the external address.
  - The gateway only trusts the `X-Forwarded-For` that comes from internal proxies, such as Traefik.
- **Errors as ProblemDetail with a `code`:** 401 without a valid token, 404 without a route, 429 with `Retry-After` above the limit and 503 when the service does not respond.
- **Rate limiting with Bucket4j, in memory:**
  - one limit per client, which is the JWT's `sub` or, without a token, the IP;
  - a lower limit for login, per IP.
- **CORS:** only for the configured origins, and none by default.
- **Shared security in `common-web`:** the 401 and the 403 as ProblemDetail, the converter for the `roles` claim and the token ignored on public routes. The gateway and the services use it.

## Alternatives considered

- **Rate limiting with Redis** (the `RequestRateLimiter` of the reactive Spring Cloud Gateway) — shares the limit between replicas, but adds a component to the stack; with one gateway replica it brings nothing.
- **Role-based authorisation in the gateway too** — would duplicate each service's rules and force a change to the gateway whenever an endpoint's roles changed.
- **A prefix per service** (e.g. `/api/v1/booking/reservations`) — exposes the decomposition into services, and moving a resource to another service would change the URL.
- **Routes as properties in the Config Server** — they could change without a new image, but the route table is part of the API design, not of the environment, and it would be harder to test.

## Consequences

### Positive

- The client sees a single API, and the decomposition into services can change without changing the URLs.
- A request without a valid token reaches no service.
- The limits and the error format are the same across the whole API.

### Negative

- With more than one gateway replica, each replica applies the limit on its own.
- A new resource requires adding its route to the gateway and, if it is public, its security rule.
- The gateway is the single entry point: if it fails, the API is unavailable until Kubernetes restarts it.
