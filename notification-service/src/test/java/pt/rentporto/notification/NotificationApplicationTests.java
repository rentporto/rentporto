package pt.rentporto.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pt.rentporto.web.testing.TestJwtDecoder;

@SpringBootTest
@Import(TestJwtDecoder.class)
class NotificationApplicationTests {

    @Test
    void contextLoads() {
    }
}
