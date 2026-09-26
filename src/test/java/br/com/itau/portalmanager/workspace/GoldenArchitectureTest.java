package br.com.itau.portalmanager.workspace;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class GoldenArchitectureTest {

    private static final String ROOT = "br.com.itau.portalmanager.workspace";
    private static final String FOUNDATION = ROOT + ".foundation..";
    private static final String FOUNDATION_CATALOG = ROOT + ".foundation.catalog..";
    private static final String CORE = ROOT + ".core..";
    private static final String FEATURE = ROOT + ".feature..";
    private static final String ENTRYPOINT = ROOT + ".entrypoint..";
    private static final Set<String> MODULE_LAYERS =
            Set.of("domain", "usecase", "repository", "integration");
    private static final Set<String> FOUNDATION_CAPABILITY_LAYERS =
            Set.of("domain", "usecase", "repository");
    private static final Set<String> RESERVED_CATALOG_SEGMENTS =
            Set.of("domain", "usecase", "repository");

    private final com.tngtech.archunit.core.domain.JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(ROOT);

    @Test
    void onlyApprovedArchitecturalZonesMayExist() {
        Set<String> allowed = Set.of("foundation", "core", "feature", "entrypoint");

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
    }

    @Test
    void compositionRootMayContainOnlyBootstrapOrSpringConfiguration() {
        var rootClasses = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().equals(ROOT))
                .toList();

        assertThat(rootClasses)
                .extracting(JavaClass::getSimpleName)
                .containsExactlyInAnyOrder(
                        "WorkspaceServiceApplication"
                );

        assertThat(rootClasses).allSatisfy(javaClass -> {
            boolean bootstrap = javaClass.isAnnotatedWith(SpringBootApplication.class);
            boolean configuration = javaClass.isAnnotatedWith(Configuration.class);

            assertThat(bootstrap || configuration)
                    .as(javaClass.getName() + " must be bootstrap or @Configuration")
                    .isTrue();
        });

        noClasses()
                .that().resideInAnyPackage(FOUNDATION, CORE, FEATURE, ENTRYPOINT)
                .should().dependOnClassesThat().resideInAPackage(ROOT)
                .check(classes);
    }

    @Test
    void catalogMustBeOrganizedByCatalogBeforeLayer() {
        String prefix = ROOT + ".foundation.catalog.";

        var invalidPackages = classes.stream()
                .map(JavaClass::getPackageName)
                .filter(packageName -> packageName.startsWith(prefix))
                .map(packageName -> packageName.substring(prefix.length()))
                .filter(relative -> !relative.equals("support"))
                .filter(relative -> !relative.startsWith("support."))
                .filter(relative -> {
                    String[] segments = relative.split("\\.");
                    if (segments.length < 2) {
                        return true;
                    }
                    String catalog = segments[0];
                    String layer = segments[1];
                    return RESERVED_CATALOG_SEGMENTS.contains(catalog)
                            || !FOUNDATION_CAPABILITY_LAYERS.contains(layer);
                })
                .distinct()
                .toList();

        assertThat(invalidPackages)
                .as("Catalog packages must follow catalog/<catalog>/domain|usecase|repository")
                .isEmpty();
    }

    @Test
    void concreteCatalogModulesMustBeExplicitTypes() {
        String prefix = ROOT + ".foundation.catalog.";

        var invalidModules = classes.stream()
                .map(JavaClass::getPackageName)
                .filter(packageName -> packageName.startsWith(prefix))
                .map(packageName -> packageName.substring(prefix.length()))
                .map(relative -> relative.substring(0, relative.indexOf('.') < 0
                        ? relative.length()
                        : relative.indexOf('.')))
                .filter(module -> !module.equals("support"))
                .filter(module -> !module.endsWith("type"))
                .distinct()
                .toList();

        assertThat(invalidModules)
                .as("Concrete catalog modules must end with 'type'")
                .isEmpty();
    }

    @Test
    void typeDomainNameMustMatchCatalogModuleName() {
        String prefix = ROOT + ".foundation.catalog.";

        var invalidTypes = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(prefix))
                .filter(javaClass -> javaClass.getPackageName().contains(".domain"))
                .filter(javaClass -> javaClass.getSimpleName().endsWith("Type"))
                .filter(javaClass -> {
                    String relative = javaClass.getPackageName().substring(prefix.length());
                    String module = relative.substring(0, relative.indexOf('.'));
                    String expected = javaClass.getSimpleName().toLowerCase(java.util.Locale.ROOT);
                    return !module.equals(expected);
                })
                .map(JavaClass::getName)
                .toList();

        assertThat(invalidTypes)
                .as("Type domain names must match their catalog module names")
                .isEmpty();
    }

    @Test
    void schemaMustFollowApprovedInternalStructure() {
        String prefix = ROOT + ".foundation.schema.";

        var invalidLayers = classes.stream()
                .map(JavaClass::getPackageName)
                .filter(packageName -> packageName.startsWith(prefix))
                .map(packageName -> packageName.substring(prefix.length()))
                .map(relative -> relative.substring(0, relative.indexOf('.') < 0
                        ? relative.length()
                        : relative.indexOf('.')))
                .filter(layer -> !FOUNDATION_CAPABILITY_LAYERS.contains(layer))
                .distinct()
                .toList();

        assertThat(invalidLayers)
                .as("invalid internal layers for foundation.schema")
                .isEmpty();
    }

    @Test
    void entitiesMustNotPersistCrossAggregateIdentifiers() {
        var invalidFields = classes.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(jakarta.persistence.Entity.class))
                .flatMap(javaClass -> javaClass.getFields().stream())
                .filter(field -> field.getName().endsWith("Identifier"))
                .filter(field -> !field.getName().equals("identifier"))
                .map(field -> field.getOwner().getName() + "." + field.getName())
                .toList();

        assertThat(invalidFields)
                .as("Cross-aggregate relations must use internal BIGINT ids; identifiers are external contracts")
                .isEmpty();
    }

    @Test
    void webResponsesMustNotExposeTechnicalIds() {
        String responsePackage = ROOT + ".entrypoint.web.";

        var invalidFields = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(responsePackage))
                .filter(javaClass -> javaClass.getPackageName().contains(".response"))
                .flatMap(javaClass -> javaClass.getFields().stream())
                .map(field -> field.getOwner().getName() + "." + field.getName())
                .filter(name -> name.endsWith(".id") || name.matches(".*\\.[a-zA-Z0-9]+Id$"))
                .toList();

        assertThat(invalidFields)
                .as("HTTP responses must expose stable identifiers, never technical database ids")
                .isEmpty();
    }

    @Test
    void restControllersMustResideInEntrypoint() {
        classes()
                .that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage(ENTRYPOINT)
                .check(classes);
    }

    @Test
    void schemaMayDependOnlyOnApprovedCatalogContracts() {
        classes()
                .that().resideInAPackage(ROOT + ".foundation.schema..")
                .should(schemaAccessOnlyApprovedCatalogs())
                .check(classes);
    }

    @Test
    void foundationMustNotDependOnHigherZones() {
        noClasses()
                .that().resideInAPackage(FOUNDATION)
                .should().dependOnClassesThat().resideInAnyPackage(CORE, FEATURE, ENTRYPOINT)
                .check(classes);
    }

    @Test
    void coreMustNotDependOnFeatureOrInput() {
        noClasses()
                .that().resideInAPackage(CORE)
                .should().dependOnClassesThat().resideInAnyPackage(FEATURE, ENTRYPOINT)
                .check(classes);
    }

    @Test
    void internalZonesMustNotDependOnInput() {
        noClasses()
                .that().resideInAnyPackage(FOUNDATION, CORE, FEATURE)
                .should().dependOnClassesThat().resideInAPackage(ENTRYPOINT)
                .check(classes);
    }

    @Test
    void domainMustRemainIndependentFromOrchestrationPersistenceIntegrationAndWeb() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..usecase..",
                        "..repository..",
                        "..integration..",
                        ENTRYPOINT,
                        "org.springframework.web.."
                )
                .check(classes);
    }

    @Test
    void entrypointMustNotBypassUseCasesIntoModuleInternals() {
        noClasses()
                .that().resideInAPackage(ENTRYPOINT)
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".core..domain..",
                        ROOT + ".core..repository..",
                        ROOT + ".core..integration..",
                        ROOT + ".feature..domain..",
                        ROOT + ".feature..repository..",
                        ROOT + ".feature..integration.."
                )
                .check(classes);
    }

    @Test
    void catalogEntrypointModulesMustBeExplicitTypes() {
        String prefix = ROOT + ".entrypoint.web.catalog.";

        var invalidModules = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(prefix))
                .filter(javaClass -> javaClass.isAnnotatedWith(RestController.class))
                .map(JavaClass::getPackageName)
                .map(packageName -> packageName.substring(prefix.length()))
                .map(relative -> relative.substring(0, relative.indexOf('.') < 0
                        ? relative.length()
                        : relative.indexOf('.')))
                .filter(module -> !module.endsWith("type"))
                .distinct()
                .toList();

        assertThat(invalidModules)
                .as("Catalog entrypoint modules must end with 'type'")
                .isEmpty();
    }

    @Test
    void catalogControllersMustRequireOwnerAuthorization() {
        String catalogWebPrefix = ROOT + ".entrypoint.web.catalog";

        var catalogControllers = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(catalogWebPrefix))
                .filter(javaClass -> javaClass.isAnnotatedWith(RestController.class))
                .toList();

        assertThat(catalogControllers).isNotEmpty();
        assertThat(catalogControllers).allSatisfy(javaClass -> {
            assertThat(javaClass.isAnnotatedWith(AuthorizationRequired.class))
                    .as(javaClass.getName() + " must declare @AuthorizationRequired")
                    .isTrue();
            assertThat(javaClass.getAnnotationOfType(AuthorizationRequired.class).level())
                    .as(javaClass.getName() + " must require OWNER")
                    .isEqualTo(AuthorizationLevel.OWNER);
        });
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
    void businessModulesMayCollaborateOnlyThroughUseCaseContracts() {
        classes()
                .that().resideInAnyPackage(CORE, FEATURE)
                .should(notAccessInternalsOfAnotherBusinessModule())
                .check(classes);
    }

    @Test
    void nestedBusinessModulesMustBeIdentifiedIndependently() {
        assertThat(businessModule(
                ROOT + ".core.configuration.workspace.usecase.create"
        )).isEqualTo("core.configuration.workspace");

        assertThat(businessModule(
                ROOT + ".core.configuration.application.repository"
        )).isEqualTo("core.configuration.application");

        assertThat(businessModule(
                ROOT + ".core.workspace.domain"
        )).isEqualTo("core.workspace");
    }

    private ArchCondition<JavaClass> schemaAccessOnlyApprovedCatalogs() {
        return new ArchCondition<>("depend only on approved catalog contracts") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    if (!targetPackage.startsWith(ROOT + ".foundation.catalog.")) {
                        continue;
                    }
                    boolean approved = targetPackage.startsWith(
                            ROOT + ".foundation.catalog.lifecycletype.domain"
                    ) || targetPackage.startsWith(
                            ROOT + ".foundation.catalog.schemascopetype.domain"
                    ) || targetPackage.startsWith(
                            ROOT + ".foundation.catalog.schemaversionstatustype.domain"
                    ) || targetPackage.startsWith(
                            ROOT + ".foundation.catalog.schematype.usecase"
                    );
                    if (!approved) {
                        events.add(SimpleConditionEvent.violated(
                                source,
                                source.getName()
                                        + " depends on non-approved catalog type "
                                        + dependency.getTargetClass().getName()
                        ));
                    }
                }
            }
        };
    }

    private ArchCondition<JavaClass> notAccessInternalsOfAnotherBusinessModule() {
        return new ArchCondition<>("collaborate with another business module only through Use Case contracts") {
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

                    if (!isPublicUseCaseContract(target)) {
                        events.add(SimpleConditionEvent.violated(
                                source,
                                source.getName()
                                        + " accesses non-public type "
                                        + target.getName()
                                        + " from module "
                                        + targetModule
                        ));
                    }
                }
            }
        };
    }

    private boolean isPublicUseCaseContract(JavaClass target) {
        if (!target.getPackageName().contains(".usecase.")) {
            return false;
        }

        String simpleName = target.getSimpleName();
        return simpleName.endsWith("CommandService")
                || simpleName.endsWith("QueryService")
                || simpleName.endsWith("Input")
                || simpleName.endsWith("Output");
    }

    private String businessModule(String packageName) {
        for (String zone : new String[]{"core", "feature"}) {
            String prefix = ROOT + "." + zone + ".";
            if (!packageName.startsWith(prefix)) {
                continue;
            }

            String remainder = packageName.substring(prefix.length());
            String[] segments = remainder.split("\\.");

            int layerIndex = -1;
            for (int index = 0; index < segments.length; index++) {
                if (MODULE_LAYERS.contains(segments[index])) {
                    layerIndex = index;
                    break;
                }
            }

            int moduleSegmentCount = layerIndex > 0 ? layerIndex : Math.min(1, segments.length);
            if (moduleSegmentCount == 0) {
                return null;
            }

            return zone + "." + String.join(
                    ".",
                    Arrays.copyOfRange(segments, 0, moduleSegmentCount)
            );
        }

        return null;
    }
}
