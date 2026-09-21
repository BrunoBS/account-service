package br.com.portalmanager.account;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class GoldenArchitectureTest {

    @Test
    void domainMustNotDependOnWebLayer() {
        var classes = new ClassFileImporter().importPackages("br.com.portalmanager.account");

        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..")
                .check(classes);
    }
}
