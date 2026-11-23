package pt.rentporto.messaging.testapp;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;

/**
 * Keeps the spans the test application finishes, in place of a Zipkin server.
 */
@Component
public class RecordedSpans extends SpanHandler {

    private final Queue<MutableSpan> finished = new ConcurrentLinkedQueue<>();

    @Override
    public boolean end(TraceContext context, MutableSpan span, Cause cause) {
        finished.add(span);
        return true;
    }

    public List<MutableSpan> ofTrace(String traceId) {
        return finished.stream().filter(span -> span.traceId().equals(traceId)).toList();
    }
}
