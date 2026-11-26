package pt.rentporto.fleet.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.domain.exception.CategoryAlreadyExistsException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.fleet.domain.model.VehicleCategory;
import pt.rentporto.fleet.domain.repository.VehicleCategories;
import pt.rentporto.fleet.testing.AcrissCodes;
import pt.rentporto.fleet.testing.FleetServiceTest;

/**
 * The primary key of the table becomes a domain exception, so the application can tell a repeated code apart.
 */
@FleetServiceTest
class JpaVehicleCategoriesTest {

    @Autowired
    private VehicleCategories categories;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void storingTheSameCodeTwiceTellsTheSecondRequest() {
        AcrissCode code = new AcrissCode(AcrissCodes.unique());
        add(code);

        assertThatThrownBy(() -> add(code)).isInstanceOf(CategoryAlreadyExistsException.class);
    }

    private void add(AcrissCode code) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> categories.add(
                VehicleCategory.create(code, "Group K - Van", new DriverRequirements(21, 2), null)));
    }
}
