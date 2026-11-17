package pt.rentporto.notification;

import org.junit.jupiter.api.Test;
import pt.rentporto.web.testing.ArchitectureRules;

class ArchitectureTest {

    @Test
    void followsTheHexagonalArchitectureOfAdr15() {
        ArchitectureRules.verify("pt.rentporto.notification");
    }
}
