package com.formpresentationreceiver.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests for the FormPresentationReceiver microservice.
 * Ensures hexagonal architecture rules, microservice isolation,
 * and pure Java/Jakarta usage in domain and application layers.
 */
class HexagonalArchitectureTest {

    private static final String BASE_PACKAGE = "com.formpresentationreceiver";
    private static final String DOMAIN_PACKAGE = BASE_PACKAGE + ".domain..";
    private static final String APPLICATION_PACKAGE = BASE_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE_PACKAGE = BASE_PACKAGE + ".infrastructure..";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Nested
    @DisplayName("Microservice isolation rules")
    class MicroserviceIsolation {

        @Test
        @DisplayName("FormPresentationReceiver classes should not depend on FormPlatform classes")
        void formpresentationreceiverShouldNotDependOnFormplatform() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("com.formpresentationreceiver..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.formplatform..");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Hexagonal architecture layer dependency rules")
    class LayerDependencies {

        @Test
        @DisplayName("Domain layer should not depend on application layer")
        void domainShouldNotDependOnApplication() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage(APPLICATION_PACKAGE);

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Domain layer should not depend on infrastructure layer")
        void domainShouldNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE);

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Application layer should not depend on infrastructure layer")
        void applicationShouldNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE);

            rule.check(importedClasses);
        }

    }

    @Nested
    @DisplayName("Domain layer purity rules - only Java and Jakarta allowed")
    class DomainLayerPurity {

        @Test
        @DisplayName("Domain classes should only use java or jakarta packages")
        void domainShouldOnlyUseJavaOrJakarta() {
            ArchRule rule = classes()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "jakarta..",
                            DOMAIN_PACKAGE
                    );

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Domain should not use Quarkus framework classes")
        void domainShouldNotUseQuarkus() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage("io.quarkus..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Domain should not use Spring framework classes")
        void domainShouldNotUseSpring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Domain should not use SLF4J directly")
        void domainShouldNotUseSlf4j() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.slf4j..");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Application layer purity rules - only Java, Jakarta, and domain allowed")
    class ApplicationLayerPurity {

        @Test
        @DisplayName("Application classes should only use java, jakarta or domain packages")
        void applicationShouldOnlyUseJavaJakartaOrDomain() {
            ArchRule rule = classes()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "jakarta..",
                            DOMAIN_PACKAGE,
                            APPLICATION_PACKAGE
                    );

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Application should not use Quarkus framework classes")
        void applicationShouldNotUseQuarkus() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage("io.quarkus..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Application should not use Spring framework classes")
        void applicationShouldNotUseSpring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Application should not use SLF4J directly")
        void applicationShouldNotUseSlf4j() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.slf4j..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Application should not use Hibernate directly")
        void applicationShouldNotUseHibernate() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.hibernate..");

            rule.check(importedClasses);
        }
    }
}
