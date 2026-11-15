package pt.rentporto.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class MessageJsonTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    static Stream<Named<EventEnvelope<?>>> messages() {
        return SampleMessages.all().stream().map(envelope -> Named.of(envelope.type(), envelope));
    }

    @ParameterizedTest
    @MethodSource("messages")
    void survivesJsonRoundTrip(EventEnvelope<?> envelope) {
        String json = MAPPER.writeValueAsString(envelope);

        EventEnvelope<?> read = MAPPER.readValue(json, envelopeTypeOf(envelope));

        assertThat(read).isEqualTo(envelope);
    }

    @ParameterizedTest
    @MethodSource("messages")
    void matchesReferenceJson(EventEnvelope<?> envelope) throws Exception {
        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(envelope));

        try (InputStream reference = MessageJsonTest.class.getResourceAsStream("/messages/" + envelope.type() + ".json")) {
            Objects.requireNonNull(reference, "missing reference JSON for " + envelope.type());
            assertThat(actual).isEqualTo(MAPPER.readTree(reference));
        }
    }

    private static JavaType envelopeTypeOf(EventEnvelope<?> envelope) {
        return MAPPER.getTypeFactory().constructParametricType(EventEnvelope.class, envelope.payload().getClass());
    }
}
