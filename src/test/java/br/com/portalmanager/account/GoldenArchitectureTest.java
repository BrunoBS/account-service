package br.com.portalmanager.account;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class GoldenArchitectureTest {

    @Test
    void domainMustRemainIndependentFromApplicationAndWeb() {
        var classes = new ClassFileImporter().importPackages("br.com.portalmanager.account");

        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..api..",
                        "..application..",
                        "..persistence..",
                        "org.springframework.web.."
                )
                .check(classes);
    }

    @Test
    void apiMustNotAccessPersistenceDirectly() {
        var classes = new ClassFileImporter().importPackages("br.com.portalmanager.account");

        noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..persistence..")
                .check(classes);
    }

    @Test
    void applicationMustNotDependOnApiLayer() {
        var classes = new ClassFileImporter().importPackages("br.com.portalmanager.account");

        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..api..")
                .check(classes);
    }
}
