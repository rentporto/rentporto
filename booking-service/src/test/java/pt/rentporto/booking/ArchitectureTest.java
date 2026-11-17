package pt.rentporto.booking;

import org.junit.jupiter.api.Test;
import pt.rentporto.web.testing.ArchitectureRules;

class ArchitectureTest {

    @Test
    void followsTheHexagonalArchitectureOfAdr15() {
        ArchitectureRules.verify("pt.rentporto.booking");
    }
}
