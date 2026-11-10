package co.com.pragma.accounts.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Reglas de arquitectura hexagonal reactiva. Importa explícitamente el
 * paquete raíz (recorre todos los jars de los submódulos en el classpath de
 * app-service) para que las reglas evalúen las clases de domain/infra.
 */
class HexagonalArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("co.com.pragma.accounts");
    }

    // El dominio son los paquetes model + ports + usecase (el "domain/" de Gradle
    // no aparece en el nombre del package Java, por eso se listan explícitamente).
    private static final String[] DOMAIN_PACKAGES = {"..model..", "..ports..", "..usecase.."};

    @Test
    void domainMustNotDependOnInfrastructure() {
        noClasses()
            .that().resideInAnyPackage(DOMAIN_PACKAGES)
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .check(classes);
    }

    @Test
    void domainMustNotDependOnFramework() {
        noClasses()
            .that().resideInAnyPackage(DOMAIN_PACKAGES)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "io.r2dbc..")
            .check(classes);
    }

    @Test
    void domainMustNotDependOnApplication() {
        noClasses()
            .that().resideInAnyPackage(DOMAIN_PACKAGES)
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .check(classes);
    }

    @Test
    void portsMustBeInterfaces() {
        classes()
            .that().resideInAPackage("..ports..")
            .should().beInterfaces()
            .check(classes);
    }

    @Test
    void usecasesMustNotUseSpringAnnotations() {
        noClasses()
            .that().resideInAPackage("..usecase..")
            .should().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
            .check(classes);
    }

    /** Prohibición central del estándar reactivo: NO @RestController. */
    @Test
    void noRestControllersAllowed() {
        noClasses()
            .that().resideInAnyPackage("co.com.pragma.accounts..")
            .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .check(classes);
    }

    @Test
    void portsMustHaveIPrefix() {
        // Todo puerto de salida usa prefijo I. El sufijo estandar es Gateway,
        // pero se admiten puertos de rol especifico (Publisher, etc.).
        classes()
            .that().resideInAPackage("..ports..")
            .should().haveSimpleNameStartingWith("I")
            .check(classes);
    }

    @Test
    void gatewaysMustHaveIPrefixAndGatewaySuffix() {
        classes()
            .that().resideInAPackage("..ports..")
            .and().haveSimpleNameEndingWith("Gateway")
            .should().haveSimpleNameStartingWith("I")
            .check(classes);
    }

    @Test
    void usecasesMustEndWithUseCase() {
        classes()
            .that().resideInAPackage("..usecase..")
            .should().haveSimpleNameEndingWith("UseCase")
            .check(classes);
    }

    @Test
    void handlersMustBeComponents() {
        // Los Handler de entrada son @Component. Se excluye el error handler global
        // (WebExceptionHandler registrado como @Bean de configuracion), que no es
        // un handler de recurso sino infraestructura transversal de WebFlux.
        classes()
            .that().resideInAPackage("..infrastructure.entrypoint..")
            .and().haveSimpleNameEndingWith("Handler")
            .and().areNotAssignableTo("org.springframework.web.server.WebExceptionHandler")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .check(classes);
    }

    @Test
    void routersMustBeConfigurations() {
        classes()
            .that().resideInAPackage("..infrastructure.entrypoint..")
            .and().haveSimpleNameEndingWith("Router")
            .should().beAnnotatedWith("org.springframework.context.annotation.Configuration")
            .check(classes);
    }

    @Test
    void entryPointsMustNotDependOnDrivenAdapters() {
        noClasses()
            .that().resideInAPackage("..infrastructure.entrypoint..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..")
            .orShould().dependOnClassesThat().resideInAPackage("..infrastructure.client..")
            .check(classes);
    }
}
