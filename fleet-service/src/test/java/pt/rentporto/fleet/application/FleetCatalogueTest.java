package pt.rentporto.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.CategoryAlreadyExistsException;
import pt.rentporto.fleet.domain.CategoryNotFoundException;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.Stations;
import pt.rentporto.fleet.domain.VehicleCategory;

class FleetCatalogueTest {

    private static final AcrissCode ECONOMY = new AcrissCode("ECMR");
    private static final DriverRequirements STANDARD = new DriverRequirements(21, 2);

    private final InMemoryVehicleCategories categories = new InMemoryVehicleCategories();
    private final RecordedEvents events = new RecordedEvents();
    private final List<VehicleCategory> published = events.categories;
    private final FleetCatalogue catalogue = new FleetCatalogue(
            mock(Stations.class), categories, events, mock(PlatformTransactionManager.class));

    @Test
    void creatingACategoryStoresItAndPublishesTheEvent() {
        VehicleCategory category = catalogue.createCategory(ECONOMY, "Group B - Economy", STANDARD, "key-1");

        assertThat(categories.withCode(ECONOMY)).contains(category);
        assertThat(published).containsExactly(category);
    }

    @Test
    void theSameKeyGetsTheSameCategoryWithoutPublishingAgain() {
        VehicleCategory first = catalogue.createCategory(ECONOMY, "Group B - Economy", STANDARD, "key-1");

        VehicleCategory again = catalogue.createCategory(ECONOMY, "Group B - Economy", STANDARD, "key-1");

        assertThat(again).isSameAs(first);
        assertThat(published).hasSize(1);
    }

    @Test
    void anotherRequestWithTheSameCodeIsAConflict() {
        catalogue.createCategory(ECONOMY, "Group B - Economy", STANDARD, "key-1");

        assertThatThrownBy(() -> catalogue.createCategory(ECONOMY, "Economy", STANDARD, "key-2"))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        assertThatThrownBy(() -> catalogue.createCategory(ECONOMY, "Economy", STANDARD, null))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        assertThat(published).hasSize(1);
    }

    @Test
    void aRepeatedRequestThatLosesTheRaceGetsTheCategoryOfTheFirst() {
        categories.createdMeanwhile = VehicleCategory.create(ECONOMY, "Group B - Economy", STANDARD, "key-1");

        VehicleCategory category = catalogue.createCategory(ECONOMY, "Group B - Economy", STANDARD, "key-1");

        assertThat(category.createdWith("key-1")).isTrue();
        assertThat(published).isEmpty();
    }

    @Test
    void anotherRequestThatLosesTheRaceIsAConflict() {
        categories.createdMeanwhile = VehicleCategory.create(ECONOMY, "Group B - Economy", STANDARD, "key-1");

        assertThatThrownBy(() -> catalogue.createCategory(ECONOMY, "Economy", STANDARD, "key-2"))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        assertThat(published).isEmpty();
    }

    @Test
    void aChangePublishesTheCurrentRequirementsAndNoChangePublishesNothing() {
        catalogue.createCategory(ECONOMY, "Group B - Economy", STANDARD, null);

        VehicleCategory changed = catalogue.changeCategory(ECONOMY, "Group B - Economy", new DriverRequirements(23, 3));
        catalogue.changeCategory(ECONOMY, "Group B - Economy", new DriverRequirements(23, 3));

        assertThat(changed.requirements()).isEqualTo(new DriverRequirements(23, 3));
        assertThat(published).hasSize(2);
        assertThat(published.getLast().requirements()).isEqualTo(new DriverRequirements(23, 3));
    }

    @Test
    void changingACategoryThatDoesNotExistIsNotFound() {
        assertThatThrownBy(() -> catalogue.changeCategory(ECONOMY, "Group B - Economy", STANDARD))
                .isInstanceOf(CategoryNotFoundException.class);
        assertThat(published).isEmpty();
    }
}
