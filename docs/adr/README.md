# Architecture decision records (ADR)

Architecture decisions of RentPorto. Each record explains the context, the decision, the alternatives and the consequences.

To record a new decision, copy `0000-template.md` to the next number and open a pull request. An accepted decision is not edited: a new ADR supersedes it.

| ADR | Decision | Status |
|---|---|---|
| [ADR-01](0001-decomposition-into-microservices.md) | Decomposition into microservices by bounded context | Accepted |
| [ADR-02](0002-hybrid-communication.md) | Hybrid communication: REST for immediate decisions, messages for state changes | Accepted |
| [ADR-03](0003-rabbitmq-as-message-broker.md) | RabbitMQ as the message broker | Accepted |
| [ADR-04](0004-service-discovery-by-kubernetes.md) | Discovery and load balancing by Kubernetes, without Eureka | Accepted |
| [ADR-05](0005-postgresql-with-a-database-per-service.md) | One PostgreSQL instance with one logical database and one user per service | Accepted |
| [ADR-06](0006-orchestrated-saga-for-reservations.md) | Orchestrated saga for the reservation, choreography for the reactions | Accepted |
| [ADR-07](0007-outbox-and-idempotent-consumers.md) | Transactional outbox and idempotent consumers | Accepted |
| [ADR-08](0008-auth-service-with-spring-security.md) | Own auth-service with Spring Security | Accepted |
| [ADR-09](0009-contract-terms-in-reservation-confirmed.md) | Contract terms frozen in ReservationConfirmed | Accepted |
| [ADR-10](0010-reserve-by-category.md) | Reserve by category and assign the vehicle at pick-up | Accepted |
| [ADR-11](0011-monorepo.md) | Monorepo with one module, one image and one pipeline per service | Accepted |
| [ADR-12](0012-stripe-behind-an-anti-corruption-layer.md) | Stripe in test mode behind an Anti-Corruption Layer, with a WireMock adapter | Superseded by ADR-29 |
| [ADR-13](0013-no-frontend.md) | No frontend: interaction through the API | Accepted |
| [ADR-14](0014-amounts-and-rounding.md) | Amounts and rounding | Proposed |
| [ADR-15](0015-modular-structure-of-the-booking-service.md) | Hexagonal architecture in the services and in the modules of the booking-service | Accepted |
| [ADR-16](0016-k3d-as-local-cluster.md) | k3d as the local Kubernetes cluster | Accepted |
| [ADR-17](0017-public-repository.md) | Public repository on GitHub | Accepted |
| [ADR-18](0018-shared-lock-in-jpa.md) | Shared lock in JPA instead of ShedLock | Accepted |
| [ADR-19](0019-external-configuration-in-config-server.md) | External configuration in the Config Server | Accepted |
| [ADR-20](0020-jwt-key-and-first-administrator.md) | JWT signing key and first administrator | Accepted |
| [ADR-21](0021-entry-through-the-api-gateway.md) | Entry through the API Gateway: routes by resource, authentication at the entrance and rate limiting | Accepted |
| [ADR-22](0022-calls-between-services-with-the-user-jwt.md) | Calls between services with the user's JWT | Accepted |
| [ADR-23](0023-deployment-on-k3d.md) | Deployment on k3d: secrets generated at deploy time, images by overlay and startup order | Accepted |
| [ADR-24](0024-metrics-with-prometheus-and-grafana.md) | Metrics with Prometheus and Grafana: pod discovery and metrics only inside the cluster | Accepted |
| [ADR-25](0025-distributed-tracing-with-zipkin.md) | Distributed tracing with Zipkin: W3C propagation and context stored in the outbox | Accepted |
| [ADR-26](0026-grafana-dashboard.md) | Grafana dashboard as code: histograms, RabbitMQ queues and business states | Accepted |
| [ADR-27](0027-swagger-ui-aggregated-in-the-gateway.md) | Swagger UI aggregated in the gateway, without the internal endpoints and with the requests through the gateway | Accepted |
| [ADR-28](0028-booking-service-autoscaling.md) | Autoscaling of the booking-service by CPU, between 2 and 3 replicas | Accepted |
| [ADR-29](0029-simulated-payments.md) | Payments with a simulated provider and test cards, instead of Stripe | Accepted |
| [ADR-30](0030-package-organisation.md) | Package organisation by kind of file in every layer, in the shared libraries and in the tests | Accepted |
