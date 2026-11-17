package pt.rentporto.gateway;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

/**
 * Starts the gateway with a WireMock server in the place of each service; the auth-service one also serves the JWK
 * Set of {@link Tokens}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableWireMock({
        @ConfigureWireMock(name = "auth-service", baseUrlProperties = "rentporto.services.auth"),
        @ConfigureWireMock(name = "customer-service", baseUrlProperties = "rentporto.services.customer"),
        @ConfigureWireMock(name = "fleet-service", baseUrlProperties = "rentporto.services.fleet"),
        @ConfigureWireMock(name = "booking-service", baseUrlProperties = "rentporto.services.booking"),
        @ConfigureWireMock(name = "rental-service", baseUrlProperties = "rentporto.services.rental"),
        @ConfigureWireMock(name = "notification-service", baseUrlProperties = "rentporto.services.notification")})
public @interface GatewayTest {
}
