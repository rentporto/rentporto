package pt.rentporto.events.topology;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps {@code infra/rabbitmq/definitions.json}, which RabbitMQ loads at startup, in line with the routing keys and
 * command queues of the message contracts.
 */
class RabbitTopologyTest {

    private static final Path DEFINITIONS = Path.of("../infra/rabbitmq/definitions.json");
    private static final String DEAD_LETTER_SUFFIX = ".dlq";

    private static final JsonNode TOPOLOGY = readDefinitions();

    @Test
    void bindsEveryRoutingKeyAndNoOtherToTheEventsExchange() {
        Set<String> boundKeys = bindings().stream()
                .filter(binding -> binding.get("source").asString().equals(Exchanges.EVENTS))
                .map(binding -> binding.get("routing_key").asString())
                .collect(Collectors.toSet());

        assertThat(boundKeys).isEqualTo(constants(RoutingKeys.class));
    }

    @Test
    void declaresEveryCommandQueue() {
        assertThat(queueNames()).containsAll(constants(CommandQueues.class));
    }

    @Test
    void bindsOnlyDeclaredQueues() {
        Set<String> destinations = bindings().stream()
                .map(binding -> binding.get("destination").asString())
                .collect(Collectors.toSet());

        assertThat(queueNames()).containsAll(destinations);
    }

    @Test
    void deadLettersEveryQueueToItsOwnDeadLetterQueue() {
        Set<String> names = queueNames();
        for (JsonNode queue : queues()) {
            String name = queue.get("name").asString();
            if (name.endsWith(DEAD_LETTER_SUFFIX)) {
                assertThat(names).as("main queue of %s", name)
                        .contains(name.substring(0, name.length() - DEAD_LETTER_SUFFIX.length()));
            } else {
                JsonNode arguments = queue.get("arguments");
                assertThat(arguments.path("x-dead-letter-exchange").asString()).as(name).isEmpty();
                assertThat(arguments.path("x-dead-letter-routing-key").asString()).as(name)
                        .isEqualTo(name + DEAD_LETTER_SUFFIX);
                assertThat(names).as("dead-letter queue of %s", name).contains(name + DEAD_LETTER_SUFFIX);
            }
        }
    }

    private static List<JsonNode> queues() {
        return StreamSupport.stream(TOPOLOGY.get("queues").spliterator(), false).toList();
    }

    private static List<JsonNode> bindings() {
        return StreamSupport.stream(TOPOLOGY.get("bindings").spliterator(), false).toList();
    }

    private static Set<String> queueNames() {
        return queues().stream().map(queue -> queue.get("name").asString()).collect(Collectors.toSet());
    }

    private static Set<String> constants(Class<?> holder) {
        return Arrays.stream(holder.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
                .map(RabbitTopologyTest::value)
                .collect(Collectors.toSet());
    }

    private static String value(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode readDefinitions() {
        try {
            return JsonMapper.builder().build().readTree(Files.readString(DEFINITIONS));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + DEFINITIONS, e);
        }
    }
}
