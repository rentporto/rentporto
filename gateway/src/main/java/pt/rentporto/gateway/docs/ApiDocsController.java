package pt.rentporto.gateway.docs;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.ObjectNode;

/**
 * The public OpenAPI document of each service, for the Swagger UI of the gateway at {@code /api/docs}. Open without a
 * token, like the documents of the services.
 */
@RestController
@Hidden
class ApiDocsController {

    static final String SERVICE_DOCS = "/api/docs/services/{service}";

    private final ServiceDocs docs;

    ApiDocsController(ServiceDocs docs) {
        this.docs = docs;
    }

    @GetMapping(value = SERVICE_DOCS, produces = MediaType.APPLICATION_JSON_VALUE)
    ObjectNode serviceDocs(@PathVariable String service) {
        return docs.publicDocs(service);
    }
}
