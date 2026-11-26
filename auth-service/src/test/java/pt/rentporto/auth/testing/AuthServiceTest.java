package pt.rentporto.auth.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Starts the service against PostgreSQL, with the administrator of {@link AuthClient} and the schema checked against
 * the entities. The tracing is on, with every trace sampled as in {@code config/application.yml}, without sending the
 * spans to Zipkin. Every test class uses the same settings, so they share one context and one database.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "rentporto.auth.admin.email=" + AuthClient.ADMIN_EMAIL,
        "rentporto.auth.admin.password=" + AuthClient.ADMIN_PASSWORD,
        "management.tracing.sampling.probability=1.0",
        "management.tracing.export.zipkin.enabled=false"})
@AutoConfigureMockMvc
@AutoConfigureTracing
@Import(PostgresContainer.class)
public @interface AuthServiceTest {
}
