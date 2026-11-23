package pt.rentporto.gateway.docs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class PublicDocsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void theInternalEndpointsAndTheSchemasOnlyTheyUseAreLeftOut() {
        ObjectNode docs = PublicDocs.of(fleetDocs(), "/api/v1");

        assertThat(docs.get("paths").propertyNames()).containsExactly("/stations", "/vehicles/{plate}");
        assertThat(docs.get("components").get("schemas").propertyNames())
                .containsExactlyInAnyOrder("StationResponse", "Address", "VehicleResponse", "ProblemDetail");
        assertThat(docs.get("components").get("securitySchemes").propertyNames()).containsExactly("bearerAuth");
    }

    @Test
    void theRequestsOfTryItOutGoThroughTheGateway() {
        ObjectNode docs = PublicDocs.of(fleetDocs(), "/api/v1");

        assertThat(docs.get("servers")).singleElement().satisfies(server -> {
            assertThat(server.get("url").stringValue()).isEqualTo("/api/v1");
            assertThat(server.get("description").stringValue()).isEqualTo("RentPorto API through the gateway");
        });
    }

    @Test
    void aDocumentWithoutPathsOrSchemasIsServedAsItIs() {
        ObjectNode docs = PublicDocs.of((ObjectNode) JSON.readTree("{\"openapi\": \"3.1.0\"}"), "/api/v1");

        assertThat(docs.get("openapi").stringValue()).isEqualTo("3.1.0");
        assertThat(docs.get("paths").isEmpty()).isTrue();
        assertThat(docs.has("components")).isFalse();
    }

    private static ObjectNode fleetDocs() {
        return (ObjectNode) JSON.readTree("""
                {
                  "openapi": "3.1.0",
                  "servers": [{"url": "http://fleet-service:8083"}],
                  "paths": {
                    "/stations": {"get": {"responses": {
                      "200": {"content": {"application/json": {"schema": {
                        "type": "array", "items": {"$ref": "#/components/schemas/StationResponse"}}}}},
                      "default": {"content": {"application/problem+json": {"schema": {
                        "$ref": "#/components/schemas/ProblemDetail"}}}}}}},
                    "/vehicles/{plate}": {"get": {"responses": {
                      "200": {"content": {"application/json": {"schema": {
                        "$ref": "#/components/schemas/VehicleResponse"}}}}}}},
                    "/internal/vehicles/assignments": {"post": {
                      "requestBody": {"content": {"application/json": {"schema": {
                        "$ref": "#/components/schemas/AssignmentRequest"}}}},
                      "responses": {"201": {"content": {"application/json": {"schema": {
                        "$ref": "#/components/schemas/VehicleResponse"}}}}}}}
                  },
                  "components": {
                    "schemas": {
                      "StationResponse": {"type": "object", "properties": {
                        "address": {"$ref": "#/components/schemas/Address"}}},
                      "Address": {"type": "object"},
                      "VehicleResponse": {"type": "object"},
                      "AssignmentRequest": {"type": "object", "properties": {
                        "details": {"$ref": "#/components/schemas/AssignmentDetails"}}},
                      "AssignmentDetails": {"type": "object"},
                      "ProblemDetail": {"type": "object"}
                    },
                    "securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer"}}
                  }
                }
                """);
    }
}
