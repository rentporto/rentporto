package pt.rentporto.web.testing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rules of ADR-15 that every service checks: dependencies point inwards, the domain does not use Spring, the
 * application reaches the outside only through its ports, and every endpoint declares its roles. Each layer keeps each
 * kind of file in its own folder (ADR-30):
 * <ul>
 *   <li>api: {@code contract} (Api), {@code controller} (Controller, Advice), {@code request} (Request),
 *       {@code response} (Response) and {@code model} (the rest);</li>
 *   <li>application: {@code service}, {@code port} (interfaces), {@code exception} and {@code model};</li>
 *   <li>domain: {@code model}, {@code repository} (interfaces), {@code service} and {@code exception};</li>
 *   <li>infrastructure: one folder per technology.</li>
 * </ul>
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

        verifyApiFolders(classes, basePackage);
        verifyLayerFolders(classes, basePackage);
    }

    private static void verifyApiFolders(JavaClasses classes, String basePackage) {
        String api = basePackage + "..api";
        String because = "the api layer keeps each kind of file in its own folder (ADR-30)";

        onlyFolders(classes, api, "contract", "controller", "request", "response", "model");

        Map.of("contract", "Api", "request", "Request", "response", "Response").forEach((folder, suffix) ->
                classes().that().resideInAPackage(api + "." + folder + "..").and().areTopLevelClasses()
                        .should().haveSimpleNameEndingWith(suffix)
                        .because(because)
                        .allowEmptyShould(true)
                        .check(classes));

        classes().that().resideInAPackage(api + ".controller..").and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("Controller").orShould().haveSimpleNameEndingWith("Advice")
                .because(because)
                .allowEmptyShould(true)
                .check(classes);

        noClasses().that().resideInAPackage(api + ".model..").and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("Api")
                .orShould().haveSimpleNameEndingWith("Controller")
                .orShould().haveSimpleNameEndingWith("Request")
                .orShould().haveSimpleNameEndingWith("Response")
                .because(because)
                .allowEmptyShould(true)
                .check(classes);

        noClasses().that().resideInAnyPackage(api + ".contract..", api + ".request..", api + ".response..",
                        api + ".model..")
                .should().dependOnClassesThat().resideInAPackage(api + ".controller..")
                .because("only the controllers implement the contracts; the rest of the api layer does not use them")
                .allowEmptyShould(true)
                .check(classes);
    }

    private static void verifyLayerFolders(JavaClasses classes, String basePackage) {
        String application = basePackage + "..application";
        String domain = basePackage + "..domain";
        String infrastructure = basePackage + "..infrastructure";
        String because = "each layer keeps each kind of file in its own folder (ADR-30)";

        onlyFolders(classes, application, "service", "port", "exception", "model");
        onlyFolders(classes, domain, "model", "repository", "service", "exception");

        noClasses().should().resideInAPackage(infrastructure)
                .because("the infrastructure has one folder per technology (ADR-30)")
                .allowEmptyShould(true)
                .check(classes);

        classes().that().areAssignableTo(Throwable.class)
                .should().resideInAPackage(basePackage + "..exception..")
                .because(because)
                .allowEmptyShould(true)
                .check(classes);

        classes().that().resideInAPackage(basePackage + "..exception..").and().areTopLevelClasses()
                .should().beAssignableTo(Throwable.class).andShould().haveSimpleNameEndingWith("Exception")
                .because(because)
                .allowEmptyShould(true)
                .check(classes);

        classes().that().resideInAnyPackage(application + ".port..", domain + ".repository..")
                .and().areTopLevelClasses()
                .should().beInterfaces()
                .because("ports and repositories are interfaces that the infrastructure implements (ADR-15, ADR-30)")
                .allowEmptyShould(true)
                .check(classes);

        noClasses().that().resideInAPackage(application + ".service..")
                .should().beInterfaces()
                .because("the interfaces of the application are its ports (ADR-30)")
                .allowEmptyShould(true)
                .check(classes);
    }

    private static void onlyFolders(JavaClasses classes, String layer, String... folders) {
        String[] packages = new String[folders.length];
        for (int i = 0; i < folders.length; i++) {
            packages[i] = layer + "." + folders[i] + "..";
        }
        classes().that().resideInAPackage(layer + "..")
                .should().resideInAnyPackage(packages)
                .because("each layer keeps each kind of file in its own folder (ADR-30)")
                .allowEmptyShould(true)
                .check(classes);
    }
}
