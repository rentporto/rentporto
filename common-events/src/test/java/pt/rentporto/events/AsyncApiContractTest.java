package pt.rentporto.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Keeps {@code docs/asyncapi/asyncapi.yaml} in line with the message contracts: it documents exactly the messages
 * that have a reference JSON, and every reference JSON and example matches its schema.
 */
class AsyncApiContractTest {

    private static final Path ASYNCAPI = Path.of("../docs/asyncapi/asyncapi.yaml");
    private static final Path REFERENCE_JSON = Path.of("src/test/resources/messages");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final SchemaRegistry SCHEMAS = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);

    private static final JsonNode DOCUMENT = readAsyncApi();

    static Stream<String> messages() throws IOException {
        try (Stream<Path> files = Files.list(REFERENCE_JSON)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted()
                    .toList()
                    .stream();
        }
    }

    @Test
    void documentsExactlyTheMessagesWithAReferenceJson() throws IOException {
        Set<String> documented = DOCUMENT.get("components").get("messages").properties().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        assertThat(documented).isEqualTo(messages().collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @MethodSource("messages")
    void referenceJsonMatchesItsSchema(String message) throws IOException {
        JsonNode reference = JSON.readTree(Files.readString(REFERENCE_JSON.resolve(message + ".json")));

        assertThat(schemaOf(message).validate(reference)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("messages")
    void exampleMatchesItsSchema(String message) {
        JsonNode example = DOCUMENT.at("/components/messages/" + message + "/examples/0/payload");

        assertThat(example.isMissingNode()).isFalse();
        assertThat(schemaOf(message).validate(example)).isEmpty();
    }

    private static Schema schemaOf(String message) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.putArray("allOf").addObject().put("$ref", "#/components/schemas/" + message + "Envelope");
        root.set("components", DOCUMENT.get("components"));
        return SCHEMAS.getSchema(root);
    }

    private static JsonNode readAsyncApi() {
        try {
            return YAMLMapper.builder().build().readTree(Files.readString(ASYNCAPI));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + ASYNCAPI, e);
        }
    }
}
