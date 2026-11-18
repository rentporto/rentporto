package pt.rentporto.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.Stations;
import pt.rentporto.fleet.domain.VehicleCategory;

class ExampleCategoriesTest {

    private final InMemoryVehicleCategories categories = new InMemoryVehicleCategories();
    private final List<VehicleCategory> published = new ArrayList<>();
    private final FleetCatalogue catalogue = new FleetCatalogue(
            mock(Stations.class), categories, published::add, mock(PlatformTransactionManager.class));
    private final ExampleCategories examples = new ExampleCategories(catalogue);

    @Test
    void anEmptyCatalogueGetsTheExamplesWithPremiumAsking25YearsAndOneEventEach() {
        examples.run(null);

        assertThat(categories.all())
                .extracting(category -> category.code().value(), VehicleCategory::name,
                        category -> category.requirements().minimumAge(),
                        category -> category.requirements().minimumLicenseYears())
                .containsExactly(
                        tuple("CDMR", "Group C - Compact", 21, 2),
                        tuple("ECMR", "Group B - Economy", 21, 2),
                        tuple("IFAR", "Group D - Intermediate SUV", 21, 2),
                        tuple("MBMR", "Group A - Mini", 21, 2),
                        tuple("PDAR", "Group E - Premium", 25, 2));
        assertThat(published).hasSize(5);
    }

    @Test
    void theNextStartupsCreateNothingAndKeepTheChangesOfTheFleetManager() {
        examples.run(null);
        catalogue.changeCategory(new AcrissCode("PDAR"), "Group E - Premium", new DriverRequirements(26, 3));
        published.clear();

        examples.run(null);

        assertThat(categories.withCode(new AcrissCode("PDAR")).orElseThrow().requirements())
                .isEqualTo(new DriverRequirements(26, 3));
        assertThat(published).isEmpty();
    }

    @Test
    void anExampleCreatedByAnotherReplicaMeanwhileIsLeftAsItIs() {
        categories.createdMeanwhile = VehicleCategory.create(
                new AcrissCode("ECMR"), "Group B - Economy", new DriverRequirements(21, 2), null);

        examples.run(null);

        assertThat(categories.all()).hasSize(5);
        assertThat(published).extracting(category -> category.code().value())
                .containsExactlyInAnyOrder("MBMR", "CDMR", "IFAR", "PDAR");
    }
}
