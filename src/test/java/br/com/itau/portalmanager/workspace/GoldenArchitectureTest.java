package br.com.itau.portalmanager.workspace;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class GoldenArchitectureTest {

    private static final String ROOT = "br.com.itau.portalmanager.workspace";
    private static final String FOUNDATION = ROOT + ".foundation..";
    private static final String CORE = ROOT + ".core..";
    private static final String FEATURE = ROOT + ".feature..";
    private static final String INPUT = ROOT + ".input..";

    private final com.tngtech.archunit.core.domain.JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(ROOT);

    @Test
    void onlyApprovedArchitecturalZonesMayExist() {
        Set<String> allowed = Set.of("foundation", "core", "feature", "input");

        var invalidPackages = classes.stream()
                .map(JavaClass::getPackageName)
                .filter(packageName -> packageName.startsWith(ROOT + "."))
                .map(packageName -> packageName.substring(ROOT.length() + 1))
                .map(relative -> relative.substring(0, relative.indexOf('.') < 0
                        ? relative.length()
                        : relative.indexOf('.')))
                .filter(zone -> !allowed.contains(zone))
                .distinct()
                .toList();

        assertThat(invalidPackages).isEmpty();

        var rootClasses = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().equals(ROOT))
                .map(JavaClass::getSimpleName)
                .toList();

        assertThat(rootClasses).containsExactly("WorkspaceServiceApplication");
    }

    @Test
    void foundationMustNotDependOnHigherZones() {
        noClasses()
                .that().resideInAPackage(FOUNDATION)
                .should().dependOnClassesThat().resideInAnyPackage(CORE, FEATURE, INPUT)
                .check(classes);
    }

    @Test
    void coreMustNotDependOnFeatureOrInput() {
        noClasses()
                .that().resideInAPackage(CORE)
                .should().dependOnClassesThat().resideInAnyPackage(FEATURE, INPUT)
                .check(classes);
    }

    @Test
    void internalZonesMustNotDependOnInput() {
        noClasses()
                .that().resideInAnyPackage(FOUNDATION, CORE, FEATURE)
                .should().dependOnClassesThat().resideInAPackage(INPUT)
                .check(classes);
    }

    @Test
    void restControllersMustNotAccessRepositories() {
        noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .check(classes);
    }

    @Test
    void restControllersMustNotAccessIntegrations() {
        noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().resideInAPackage("..integration..")
                .check(classes);
    }

    @Test
    void domainMustNotDependOnIntegration() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..integration..")
                .check(classes);
    }

    @Test
    void businessModulesMustNotAccessInternalsOfAnotherBusinessModule() {
        classes()
                .that().resideInAnyPackage(CORE, FEATURE)
                .should(notAccessRepositoryOrDomainOfAnotherModule())
                .check(classes);
    }

    private ArchCondition<JavaClass> notAccessRepositoryOrDomainOfAnotherModule() {
        return new ArchCondition<>("not access repository or domain of another business module") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                String sourceModule = businessModule(source.getPackageName());
                if (sourceModule == null) {
                    return;
                }

                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetModule = businessModule(target.getPackageName());

                    if (targetModule == null || targetModule.equals(sourceModule)) {
                        continue;
                    }

                    String targetPackage = target.getPackageName();
                    boolean internal = targetPackage.contains(".repository")
                            || targetPackage.contains(".domain");

                    if (internal) {
                        events.add(SimpleConditionEvent.violated(
                                source,
                                source.getName() + " accesses internal type " + target.getName()
                        ));
                    }
                }
            }
        };
    }

    private String businessModule(String packageName) {
        for (String zone : new String[]{"core", "feature"}) {
            String prefix = ROOT + "." + zone + ".";
            if (!packageName.startsWith(prefix)) {
                continue;
            }

            String remainder = packageName.substring(prefix.length());
            int separator = remainder.indexOf('.');
            String module = separator < 0 ? remainder : remainder.substring(0, separator);
            return zone + "." + module;
        }
        return null;
    }
}
