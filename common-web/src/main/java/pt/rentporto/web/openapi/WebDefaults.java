package pt.rentporto.web.openapi;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Springdoc settings every service needs, added with the lowest precedence: responses are documented as JSON, and the
 * keys of the OpenAPI document are sorted, so the committed {@code docs/openapi/{service}.yaml} only changes when the
 * contract does.
 */
public class WebDefaults implements EnvironmentPostProcessor {

    private static final Map<String, Object> DEFAULTS = Map.of(
            "springdoc.default-produces-media-type", "application/json",
            "springdoc.writer-with-order-by-keys", "true");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().addLast(new MapPropertySource("rentportoWebDefaults", DEFAULTS));
    }
}
