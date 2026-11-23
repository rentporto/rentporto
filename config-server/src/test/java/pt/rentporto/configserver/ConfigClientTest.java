package pt.rentporto.configserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Starts a client with the real {@code application.yml} of the customer-service and checks what it gets from the
 * Config Server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigClientTest {

    private static final String SERVICE_CONFIG = "file:../customer-service/src/main/resources/application.yml";

    @LocalServerPort
    private int port;

    @ParameterizedTest
    @CsvSource({
            "dev, jdbc:postgresql://localhost:5432/customer",
            "k8s, jdbc:postgresql://postgres:5432/customer"})
    void loadsTheConfigurationOfItsProfile(String profile, String datasourceUrl) {
        try (ConfigurableApplicationContext client = startClient(profile, "http://localhost:" + port)) {
            assertThat(client.getEnvironment().getProperty("spring.datasource.url")).isEqualTo(datasourceUrl);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "dev, http://localhost:9411/api/v2/spans",
            "k8s, http://zipkin:9411/api/v2/spans"})
    void sendsEveryTraceToTheZipkinOfItsProfile(String profile, String zipkinEndpoint) {
        try (ConfigurableApplicationContext client = startClient(profile, "http://localhost:" + port)) {
            assertThat(client.getEnvironment().getProperty("management.tracing.export.zipkin.endpoint"))
                    .isEqualTo(zipkinEndpoint);
            assertThat(client.getEnvironment().getProperty("management.tracing.sampling.probability"))
                    .isEqualTo("1.0");
        }
    }

    @ParameterizedTest
    @CsvSource({"dev", "k8s"})
    void recordsTheLatencyOfTheRequestsInAHistogramForTheDashboard(String profile) {
        try (ConfigurableApplicationContext client = startClient(profile, "http://localhost:" + port)) {
            assertThat(client.getEnvironment().getProperty(
                    "management.metrics.distribution.percentiles-histogram.http.server.requests")).isEqualTo("true");
        }
    }

    @Test
    void doesNotStartWithoutTheConfigServer() throws IOException {
        String unavailable = "http://localhost:" + freePort();

        assertThatThrownBy(() -> startClient("dev", unavailable).close())
                .hasStackTraceContaining("Could not locate PropertySource");
    }

    private static ConfigurableApplicationContext startClient(String profile, String configServer) {
        String testImport = System.clearProperty("rentporto.config.import");
        try {
            return new SpringApplicationBuilder(Client.class)
                    .web(WebApplicationType.NONE)
                    .run(
                            "--spring.config.location=" + SERVICE_CONFIG,
                            "--spring.profiles.active=" + profile,
                            "--CONFIG_SERVER_URL=" + configServer,
                            "--spring.cloud.config.enabled=true");
        } finally {
            if (testImport != null) {
                System.setProperty("rentporto.config.import", testImport);
            }
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    static class Client {
    }
}
