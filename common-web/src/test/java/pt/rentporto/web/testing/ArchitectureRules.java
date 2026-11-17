package pt.rentporto.web.testing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rules of ADR-15 that every service checks: dependencies point inwards, the domain does not use Spring, the
 * application reaches the outside only through its ports, and every endpoint declares its roles.
 */
public final class ArchitectureRules {

    private ArchitectureRules() {
    }

    /**
     * Checks the classes of a service. In the booking-service, the layers are inside each module.
     *
     * @param basePackage package of the service, e.g. {@code pt.rentporto.customer}
     */
    public static void verify(String basePackage) {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(basePackage);

        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("API").definedBy(basePackage + "..api..")
                .layer("Application").definedBy(basePackage + "..application..")
                .layer("Domain").definedBy(basePackage + "..domain..")
                .layer("Infrastructure").definedBy(basePackage + "..infrastructure..")
                .whereLayer("API").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("API", "Infrastructure")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("API", "Application", "Infrastructure")
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .check(classes);

        noClasses().that().resideInAPackage(basePackage + "..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .allowEmptyShould(true)
                .check(classes);

        noClasses().that().resideInAPackage(basePackage + "..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.data..",
                        "org.springframework.dao..",
                        "org.springframework.security..",
                        "org.springframework.web..",
                        "org.springframework.amqp..",
                        "org.springframework.cloud.openfeign..",
                        "jakarta.persistence..")
                .allowEmptyShould(true)
                .check(classes);

        methods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .should().beAnnotatedWith(PreAuthorize.class)
                .because("every endpoint declares its roles, or permitAll() if it is public")
                .allowEmptyShould(true)
                .check(classes);
    }
}
