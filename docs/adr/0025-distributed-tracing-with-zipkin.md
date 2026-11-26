# ADR-25: Distributed tracing with Zipkin

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-07, ADR-16, ADR-19, ADR-22, ADR-24; backlog RP-50

## Context

A request crosses the gateway, one or more services over REST and, through RabbitMQ, the event consumers. To diagnose latency and failures, we want to see that path as a single trace, with the same traceId from start to finish.

- **Asynchronous publishing:** with the outbox (ADR-07), the message is written in the request's transaction and sent later by the publisher, on another thread and without the request's context. Without care, each message starts a new trace.
- **Calls between services:** the gateway routes the requests, and the rental-service calls the fleet-service and payments through Feign (ADR-22).
- **Budget:** the local cluster has about 10 GB of memory for everything (ADR-16).

## Decision

- **Brave and Zipkin:** each application uses `spring-boot-starter-zipkin`, which in Spring Boot 4 brings Micrometer Tracing with Brave and sends the spans to Zipkin. Each service's name in Zipkin is its `spring.application.name`.
- **W3C propagation:** the context travels in the `traceparent` header, in HTTP requests and in AMQP messages. The logs carry the traceId and the spanId.
- **100 % sampling:** `management.tracing.sampling.probability: 1.0` in `config/application.yml`, for every profile. With the default value, 10 %, most requests were not recorded.
- **The outbox keeps the request's context:**
  - the OutboxWriter writes the `traceparent` of the current span to the outbox's `headers` column, which already existed;
  - the OutboxPublisher publishes each message inside an `outbox publish` span, a child of that context;
  - the RabbitTemplate's observation puts the `traceparent` on the message, and the listeners' observation continues the trace in the consumer. The `common-messaging` defaults turn both on.
- **Feign:** a service that calls another through Feign adds `feign-micrometer`, which passes the trace on in the call.
- **Actuator kept out of the traces:** `common-web` does not observe requests to `/actuator` or what happens while it serves them. The Kubernetes probes and Prometheus call each pod's actuator every 10 to 15 seconds, and their traces would hide the users' requests and fill Zipkin's memory.
- **Zipkin in memory:**
  - in compose and on k3d, Zipkin keeps the traces in memory; on k3d, up to 50 000 spans;
  - on k3d it is opened through `kubectl port-forward`, like Grafana (ADR-24);
  - `deploy.sh` confirms that Zipkin has the trace of the login through the gateway and the auth-service.
- **Tests as in production:** the services' test annotations turn tracing on with `@AutoConfigureTracing` and 100 % sampling, without sending to Zipkin. Without the annotation, Spring Boot turns off the sending in tests and, with it, the propagation.

## Alternatives considered

- **OpenTelemetry with the Zipkin exporter** — makes it possible to switch backend without changing the applications, but brings the OpenTelemetry SDK and more configuration, for the same result with a single backend.
- **TraceId in the event envelope** — avoids the header, but mixes observability with the event contract and forces each listener to continue the trace by hand. The envelope keeps the `correlationId`, which links the messages of the same business flow (ADR-07).
- **Publishing without keeping the context** — it is the simplest, but each message starts a new trace and the link between the request and the reactions to its events is lost.
- **Zipkin with persistent storage** (Elasticsearch, Cassandra or MySQL) — keeps the traces across restarts, but one more server does not fit the local cluster's budget.

## Consequences

### Positive

- A request shows up in Zipkin with the same traceId from the gateway to the message consumers, with the wait in the outbox in plain view.
- The logs of every service carry the traceId, so the logs of a request can be put together.
- A new service only needs the dependency: the outbox and RabbitMQ already carry the trace.

### Negative

- With 100 % sampling, every request produces spans; with real traffic, the sampling would have to go down.
- The traces are lost when Zipkin restarts.
- A message written outside a request, for example by a scheduler, starts a trace of its own.
- A Feign call without `feign-micrometer` ends up in a separate trace.
- The `http.server.requests` metrics no longer count the requests to the actuator.
- On k3d, the Zipkin endpoint and the sampling come from `config/` on `main` (ADR-19); before the merge, the cluster does not have them.
