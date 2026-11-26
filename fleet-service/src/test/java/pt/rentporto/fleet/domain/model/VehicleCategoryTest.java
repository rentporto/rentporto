package pt.rentporto.fleet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VehicleCategoryTest {

    private static final DriverRequirements STANDARD = new DriverRequirements(21, 2);

    private final VehicleCategory economy =
            VehicleCategory.create(new AcrissCode("ECMR"), " Group B - Economy ", STANDARD, "key-1");

    @Test
    void theNameIsStoredWithoutSurroundingSpaces() {
        assertThat(economy.name()).isEqualTo("Group B - Economy");
    }

    @Test
    void aChangeWithTheSameNameAndRequirementsChangesNothing() {
        assertThat(economy.change("Group B - Economy ", new DriverRequirements(21, 2))).isFalse();
    }

    @Test
    void aNewNameOrNewRequirementsAreAChange() {
        assertThat(economy.change("Group B - Economy Plus", STANDARD)).isTrue();
        assertThat(economy.name()).isEqualTo("Group B - Economy Plus");

        assertThat(economy.change("Group B - Economy Plus", new DriverRequirements(21, 3))).isTrue();
        assertThat(economy.requirements()).isEqualTo(new DriverRequirements(21, 3));
    }

    @Test
    void onlyTheKeyOfTheRequestThatCreatedItRepeatsIt() {
        assertThat(economy.createdWith("key-1")).isTrue();
        assertThat(economy.createdWith("key-2")).isFalse();
        assertThat(economy.createdWith(null)).isFalse();
        assertThat(VehicleCategory.create(new AcrissCode("CDMR"), "Group C - Compact", STANDARD, null)
                .createdWith(null)).isFalse();
    }
}
