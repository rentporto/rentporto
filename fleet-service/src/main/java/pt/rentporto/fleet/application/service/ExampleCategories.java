package pt.rentporto.fleet.application.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pt.rentporto.fleet.domain.exception.CategoryAlreadyExistsException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.fleet.domain.model.VehicleCategory;

/**
 * Creates the example categories at startup through {@link FleetCatalogue}, so that each one publishes
 * {@code VehicleCategoryChanged} and the booking-service gets them like any other. Only the missing ones are created:
 * a change made by a fleet manager stays, and when another replica creates one first, this one leaves it as it is.
 */
@Component
class ExampleCategories implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExampleCategories.class);

    private static final List<Example> EXAMPLES = List.of(
            new Example("MBMR", "Group A - Mini", 21, 2),
            new Example("ECMR", "Group B - Economy", 21, 2),
            new Example("CDMR", "Group C - Compact", 21, 2),
            new Example("IFAR", "Group D - Intermediate SUV", 21, 2),
            new Example("PDAR", "Group E - Premium", 25, 2));

    private final FleetCatalogue catalogue;

    ExampleCategories(FleetCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<AcrissCode> existing = catalogue.categories().stream()
                .map(VehicleCategory::code)
                .collect(Collectors.toSet());
        for (Example example : EXAMPLES) {
            AcrissCode code = new AcrissCode(example.code());
            if (existing.contains(code)) {
                continue;
            }
            try {
                catalogue.createCategory(code, example.name(),
                        new DriverRequirements(example.minimumAge(), example.minimumLicenseYears()), null);
                log.info("Created the example category {}", code);
            } catch (CategoryAlreadyExistsException e) {
                log.info("The example category {} was created by another replica", code);
            }
        }
    }

    private record Example(String code, String name, int minimumAge, int minimumLicenseYears) {
    }
}
