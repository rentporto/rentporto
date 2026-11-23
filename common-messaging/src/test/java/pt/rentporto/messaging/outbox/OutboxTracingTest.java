package pt.rentporto.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import brave.Span.Kind;
import brave.handler.MutableSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.events.reservation.ReservationExpired;
import pt.rentporto.messaging.testapp.MessagingTestApplication;
import pt.rentporto.messaging.testapp.RecordedSpans;
import pt.rentporto.messaging.testapp.RecordingListener;
import pt.rentporto.messaging.testapp.TestMessages;

/**
 * A message keeps the trace of the request that wrote it to the outbox, although the publisher sends it later on
 * another thread. Tests leave the export of the spans off, and with it their propagation, so this one turns it on and
 * samples every trace, as {@code config/application.yml} does; the spans go to {@link RecordedSpans}.
 */
@SpringBootTest(classes = MessagingTestApplication.class, properties = "management.tracing.sampling.probability=1.0")
@AutoConfigureTracing
class OutboxTracingTest {

    @Autowired
    private OutboxWriter outbox;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Tracer tracer;

    @Autowired
    private RecordingListener listener;

    @Autowired
    private RecordedSpans spans;

    @Test
    void theMessageAndItsConsumerCarryTheTraceOfTheRequestThatWroteIt() {
        EventEnvelope<ReservationExpired> message = TestMessages.reservationExpired("PAYMENT_TIMEOUT");
        Span request = tracer.nextSpan().name("request").start();
        try (Tracer.SpanInScope scope = tracer.withSpan(request)) {
            write(message);
        } finally {
            request.end();
        }
        String traceId = request.context().traceId();

        await().atMost(Duration.ofSeconds(10)).until(() -> listener.traceparent(message.eventId()).isPresent());
        assertThat(listener.traceparent(message.eventId())).get().asString().startsWith("00-" + traceId + "-");
        await().atMost(Duration.ofSeconds(5)).until(() ->
                spans.ofTrace(traceId).stream().anyMatch(span -> span.kind() == Kind.CONSUMER));
        assertThat(spans.ofTrace(traceId)).extracting(MutableSpan::name).contains(OutboxTracing.PUBLISH_SPAN);
    }

    @Test
    void aMessageWrittenOutsideARequestStartsATraceOfItsOwn() {
        EventEnvelope<ReservationExpired> first = TestMessages.reservationExpired("PAYMENT_TIMEOUT");
        EventEnvelope<ReservationExpired> second = TestMessages.reservationExpired("PAYMENT_TIMEOUT");

        write(first);
        write(second);

        await().atMost(Duration.ofSeconds(10)).until(() ->
                listener.traceparent(first.eventId()).isPresent() && listener.traceparent(second.eventId()).isPresent());
        assertThat(traceId(listener.traceparent(first.eventId()).orElseThrow()))
                .isNotEqualTo(traceId(listener.traceparent(second.eventId()).orElseThrow()));
    }

    private void write(EventEnvelope<ReservationExpired> message) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                outbox.publish(RoutingKeys.RESERVATION_EXPIRED, message));
    }

    private static String traceId(String traceparent) {
        return traceparent.split("-")[1];
    }
}
