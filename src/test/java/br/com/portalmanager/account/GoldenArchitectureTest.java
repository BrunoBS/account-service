package br.com.portalmanager.account;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class GoldenArchitectureTest {

    private static final String API = "br.com.portalmanager.account.api..";
    private static final String APPLICATION = "br.com.portalmanager.account.application..";
    private static final String DOMAIN = "br.com.portalmanager.account.domain..";
    private static final String PERSISTENCE = "br.com.portalmanager.account.persistence..";

    @Test
    void domainMustRemainIndependentFromApplicationAndWeb() {
        var classes = new ClassFileImporter().importPackages("br.com.portalmanager.account");

        noClasses()
                .that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(
                        API,
                        APPLICATION,
                        PERSISTENCE,
                        "org.springframework.web.."
                )
                .check(classes);
    }

    @Test
    void apiMustNotAccessPersistenceDirectly() {
        var classes = new ClassFileImporter().importPackages("br.com.portalmanager.account");

        noClasses()
                .that().resideInAPackage(API)
                .should().dependOnClassesThat().resideInAPackage(PERSISTENCE)
                .check(classes);
    }

    @Test
    void applicationMustNotDependOnApiLayer() {
        var classes = new ClassFileImporter().importPackages("br.com.portalmanager.account");

        noClasses()
                .that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat().resideInAPackage(API)
                .check(classes);
    }
}
