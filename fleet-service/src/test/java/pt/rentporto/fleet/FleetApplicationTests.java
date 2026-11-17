package pt.rentporto.fleet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pt.rentporto.web.testing.TestJwtDecoder;

@SpringBootTest
@Import(TestJwtDecoder.class)
class FleetApplicationTests {

    @Test
    void contextLoads() {
    }
}
