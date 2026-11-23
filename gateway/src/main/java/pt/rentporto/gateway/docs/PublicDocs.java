package pt.rentporto.gateway.docs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Turns the OpenAPI document of a service into what the clients reach through the gateway: without the internal
 * endpoints and the schemas only they use, and with the public prefix as the server, so that "Try it out" goes
 * through the gateway.
 */
final class PublicDocs {

    private static final String INTERNAL = "/internal";
    private static final String SCHEMA_REFERENCE = "#/components/schemas/";

    private PublicDocs() {
    }

    /**
     * Keeps the public part of a document.
     *
     * @param docs   OpenAPI document of a service, changed in place
     * @param server URL the clients send the requests to, e.g. {@code /api/v1}
     * @return the document
     */
    static ObjectNode of(ObjectNode docs, String server) {
        ObjectNode paths = docs.get("paths") instanceof ObjectNode found ? found : docs.putObject("paths");
        paths.remove(paths.propertyNames().stream().filter(path -> path.startsWith(INTERNAL)).toList());
        docs.putArray("servers").addObject()
                .put("url", server)
                .put("description", "RentPorto API through the gateway");
        if (docs.path("components").get("schemas") instanceof ObjectNode schemas) {
            schemas.retain(referenced(paths, schemas));
        }
        return docs;
    }

    private static Set<String> referenced(JsonNode paths, ObjectNode schemas) {
        Set<String> names = new HashSet<>();
        Deque<JsonNode> pending = new ArrayDeque<>();
        pending.push(paths);
        while (!pending.isEmpty()) {
            JsonNode node = pending.pop();
            JsonNode reference = node.get("$ref");
            if (reference != null && reference.isString() && reference.stringValue().startsWith(SCHEMA_REFERENCE)) {
                String name = reference.stringValue().substring(SCHEMA_REFERENCE.length());
                if (names.add(name) && schemas.get(name) != null) {
                    pending.push(schemas.get(name));
                }
            }
            node.values().forEach(pending::push);
        }
        return names;
    }
}
