# ADR-27: Swagger UI aggregated in the gateway

- **Status:** Accepted
- **Date:** 2026-11-23
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-21, ADR-24; backlog RP-52

## Context

Each service publishes its OpenAPI specification, but clients only reach the gateway (ADR-21). We want the documentation of every service on a single page, to explore the API without knowing where each service is.

- **Internal endpoints:** the customer-service, the fleet-service and the booking-service have `/internal/**` endpoints, called between services, which are in their specifications but cannot be reached from outside.
- **Public prefix:** the gateway strips `/api/v1` before routing, so the paths in the specifications do not have it. With nothing else, "Try it out" would call `/stations` instead of `/api/v1/stations`.
- **Ingress:** on k3d, the Ingress only routes `/api` (ADR-24).

## Decision

- **The gateway serves the specifications:**
  - `GET /api/docs/services/{service}` reads the service's `/v3/api-docs` at request time, so it always shows the version that is running;
  - it strips the `/internal/**` paths and the schemas that only they use;
  - it sets `/api/v1` as the server, so "Try it out" goes through the gateway, with the token from "Authorize".
- **The gateway's Swagger UI:** the gateway's springdoc lists the six services that have an API. The page opens at `/api/docs`, which redirects to `/api/swagger-ui`, without a token.
- **Everything under `/api`:** the Ingress still routes only `/api`, and the page opens on k3d without changing it.
- **A stopped service** answers 503 for its specification; the others remain available.
- **The complete contracts** remain in `docs/openapi/`, with the internal endpoints, for the team.

## Alternatives considered

- **springdoc groups in each service**, a public one without `/internal/**` — each service would filter its own specification, but the server would still be wrong when read through the gateway, and the same configuration would be repeated in six services.
- **Gateway routes to each service's `/v3/api-docs`** — needs no code, but does not strip the internal endpoints, and the rewritten path loses the prefix that springdoc uses for the server.
- **Copying the contracts from `docs/openapi/` into the gateway at build time** — avoids the requests to the services, but the page shows the version from the gateway's build, not that of the running services.
- **Adding `/swagger-ui` and `/v3/api-docs` to the Ingress** — it is springdoc's default path, but it opens paths outside `/api`, which ADR-24 wants to avoid.

## Consequences

### Positive

- A single page for the whole API, the same in compose and on k3d.
- The internal endpoints and their schemas are not shown outside.
- "Try it out" works against the real API, through the gateway.

### Negative

- The gateway now knows which services have an API, in one list in the code and another in the springdoc configuration; a test confirms that the two match.
- Every time a specification is opened, a request goes to the service.
- The filter relies on the `/internal/**` convention: an internal endpoint on another path would show up.
