package pt.rentporto.auth;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Checks the hexagonal architecture of ADR-15: dependencies point inwards, the domain does not use Spring, and the
 * application reaches the outside only through its ports.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("pt.rentporto.auth");

    @Test
    void dependenciesPointInwards() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("API").definedBy("..auth.api..")
                .layer("Application").definedBy("..auth.application..")
                .layer("Domain").definedBy("..auth.domain..")
                .layer("Infrastructure").definedBy("..auth.infrastructure..")
                .whereLayer("API").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("API", "Infrastructure")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("API", "Application", "Infrastructure")
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .check(CLASSES);
    }

    @Test
    void theDomainDoesNotUseSpring() {
        noClasses().that().resideInAPackage("..auth.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(CLASSES);
    }

    @Test
    void theApplicationReachesTheOutsideOnlyThroughItsPorts() {
        noClasses().that().resideInAPackage("..auth.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.data..",
                        "org.springframework.dao..",
                        "org.springframework.security..",
                        "org.springframework.web..",
                        "org.springframework.amqp..",
                        "org.springframework.cloud.openfeign..",
                        "jakarta.persistence..")
                .check(CLASSES);
    }
}
