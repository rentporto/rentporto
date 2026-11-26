package pt.rentporto.web.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;

/**
 * Parts of the OpenAPI document that are the same in every service: the title, the Bearer JWT security scheme
 * (required unless an operation declares an empty {@code @SecurityRequirements}) and the ProblemDetail returned for
 * any error.
 */
public class OpenApiContract implements OpenApiCustomizer {

    static final String BEARER_AUTH = "bearerAuth";
    static final String PROBLEM = "ProblemDetail";
    static final String PROBLEM_JSON = "application/problem+json";

    private final String title;

    /**
     * Creates the customizer.
     *
     * @param title title of the document, the service name
     */
    public OpenApiContract(String title) {
        this.title = title;
    }

    @Override
    public void customise(OpenAPI openApi) {
        openApi.info(new Info().title(title).version("v1"));
        if (openApi.getComponents() == null) {
            openApi.components(new Components());
        }
        openApi.getComponents()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSchemas(PROBLEM, problemSchema());
        openApi.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
        if (openApi.getPaths() != null) {
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                if (!operation.getResponses().containsKey("default")) {
                    operation.getResponses().addApiResponse("default", problemResponse());
                }
            }));
        }
    }

    private static Schema<?> problemSchema() {
        ObjectSchema invalidField = new ObjectSchema();
        invalidField.addProperty("field", new StringSchema());
        invalidField.addProperty("message", new StringSchema());

        ObjectSchema problem = new ObjectSchema();
        problem.description("Error response (RFC 9457)");
        problem.addProperty("type", new StringSchema().format("uri"));
        problem.addProperty("title", new StringSchema());
        problem.addProperty("status", new IntegerSchema());
        problem.addProperty("detail", new StringSchema());
        problem.addProperty("instance", new StringSchema().format("uri"));
        problem.addProperty("code", new StringSchema().description("Reason of the error, e.g. QUOTE_EXPIRED"));
        problem.addProperty("errors", new ArraySchema().items(invalidField).description("Invalid fields"));
        return problem;
    }

    private static ApiResponse problemResponse() {
        return new ApiResponse()
                .description("Error")
                .content(new Content().addMediaType(PROBLEM_JSON, new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM))));
    }
}
