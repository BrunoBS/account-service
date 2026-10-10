package br.com.portalmanager.platform.workspace;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

class GoldenArchitectureTest {

    private static final String ROOT = "br.com.portalmanager.platform.workspace";
    private static final String FOUNDATION = ROOT + ".foundation..";
    private static final String CORE = ROOT + ".core..";
    private static final String FEATURE = ROOT + ".feature..";
    private static final String ENTRYPOINT = ROOT + ".entrypoint..";
    private static final Set<String> FOUNDATION_CAPABILITY_LAYERS = Set.of(
        "domain",
        "usecase",
        "repository",
        "integration",
        "facade"
    );
    private static final Set<String> RESERVED_CATALOG_SEGMENTS = Set.of("domain", "usecase", "repository", "facade");

    private final com.tngtech.archunit.core.domain.JavaClasses classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(ROOT);

    @Test
    void onlyApprovedArchitecturalZonesMayExist() {
        Set<String> allowed = Set.of("foundation", "core", "feature", "entrypoint");
        var invalidPackages = classes
            .stream()
            .map(JavaClass::getPackageName)
            .filter(p -> p.startsWith(ROOT + "."))
            .map(p -> p.substring(ROOT.length() + 1))
            .map(r -> r.substring(0, r.indexOf('.') < 0 ? r.length() : r.indexOf('.')))
            .filter(z -> !allowed.contains(z))
            .distinct()
            .toList();
        assertThat(invalidPackages).isEmpty();
    }

    @Test
    void compositionRootMayContainOnlyBootstrapOrSpringConfiguration() {
        var rootClasses = classes
            .stream()
            .filter(c -> c.getPackageName().equals(ROOT))
            .toList();
        assertThat(rootClasses)
            .extracting(JavaClass::getSimpleName)
            .containsExactlyInAnyOrder("WorkspaceServiceApplication");
        assertThat(rootClasses).allSatisfy(c ->
            assertThat(
                c.isAnnotatedWith(SpringBootApplication.class) || c.isAnnotatedWith(Configuration.class)
            ).isTrue()
        );
        noClasses()
            .that()
            .resideInAnyPackage(FOUNDATION, CORE, FEATURE, ENTRYPOINT)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(ROOT)
            .check(classes);
    }

    @Test
    void catalogMustBeOrganizedByCatalogBeforeLayer() {
        String prefix = ROOT + ".foundation.catalog.";
        var invalid = classes
            .stream()
            .map(JavaClass::getPackageName)
            .filter(p -> p.startsWith(prefix))
            .map(p -> p.substring(prefix.length()))
            .filter(r -> !r.equals("integration") && !r.startsWith("integration."))
            .filter(r -> {
                String[] s = r.split("\\.");
                return (
                    s.length < 2 ||
                    RESERVED_CATALOG_SEGMENTS.contains(s[0]) ||
                    !FOUNDATION_CAPABILITY_LAYERS.contains(s[1])
                );
            })
            .distinct()
            .toList();
        assertThat(invalid)
            .as("Catalog packages must follow catalog/<catalog>/domain|usecase|repository|facade")
            .isEmpty();
    }

    @Test
    void concreteCatalogModulesMustBeExplicitTypes() {
        String prefix = ROOT + ".foundation.catalog.";
        var invalid = classes
            .stream()
            .map(JavaClass::getPackageName)
            .filter(p -> p.startsWith(prefix))
            .map(p -> p.substring(prefix.length()))
            .map(r -> r.substring(0, r.indexOf('.') < 0 ? r.length() : r.indexOf('.')))
            .filter(m -> !m.equals("integration"))
            .filter(m -> !m.endsWith("type"))
            .distinct()
            .toList();
        assertThat(invalid).isEmpty();
    }

    @Test
    void schemaMustFollowApprovedInternalStructure() {
        String prefix = ROOT + ".foundation.schema.";
        var invalid = classes
            .stream()
            .map(JavaClass::getPackageName)
            .filter(p -> p.startsWith(prefix))
            .map(p -> p.substring(prefix.length()))
            .map(r -> r.substring(0, r.indexOf('.') < 0 ? r.length() : r.indexOf('.')))
            .filter(l -> !FOUNDATION_CAPABILITY_LAYERS.contains(l))
            .distinct()
            .toList();
        assertThat(invalid).isEmpty();
    }

    @Test
    void restControllersMustResideInEntrypoint() {
        classes().that().areAnnotatedWith(RestController.class).should().resideInAPackage(ENTRYPOINT).check(classes);
    }

    @Test
    void foundationMustNotDependOnHigherZones() {
        noClasses()
            .that()
            .resideInAPackage(FOUNDATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(CORE, FEATURE, ENTRYPOINT)
            .check(classes);
    }

    @Test
    void coreMustNotDependOnFeatureOrInput() {
        noClasses()
            .that()
            .resideInAPackage(CORE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FEATURE, ENTRYPOINT)
            .check(classes);
    }

    @Test
    void internalZonesMustNotDependOnInput() {
        noClasses()
            .that()
            .resideInAnyPackage(FOUNDATION, CORE, FEATURE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(ENTRYPOINT)
            .check(classes);
    }

    @Test
    void catalogFacadesMustRequireOwnerAuthorization() {
        String prefix = ROOT + ".foundation.catalog.";
        var facades = classes
            .stream()
            .filter(c -> c.getPackageName().startsWith(prefix) && c.getPackageName().endsWith(".facade"))
            .toList();
        assertThat(facades).isNotEmpty();
        assertThat(facades).allSatisfy(c -> {
            var annotated = Arrays.stream(c.reflect().getMethods())
                .filter(m -> m.isAnnotationPresent(AuthorizationRequired.class))
                .toList();
            assertThat(annotated)
                .as(c.getName() + " must expose authorized facade methods")
                .isNotEmpty();
            assertThat(annotated).allSatisfy(m ->
                assertThat(m.getAnnotation(AuthorizationRequired.class).level()).isEqualTo(AuthorizationLevel.OWNER)
            );
        });
    }

    @Test
    void restControllersMustNotAccessRepositories() {
        noClasses()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repository..")
            .check(classes);
    }

    @Test
    void restControllersMustNotAccessIntegrations() {
        noClasses()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..integration..")
            .check(classes);
    }

    @Test
    void domainMustRemainIndependentFromOrchestrationPersistenceIntegrationAndWeb() {
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..usecase..",
                "..repository..",
                "..integration..",
                ENTRYPOINT,
                "org.springframework.web.."
            )
            .check(classes);
    }

    @Test
    void restEndpointsMustNotExposeUseCaseModels() {
        var invalid = classes
            .stream()
            .filter(c -> c.isAnnotatedWith(RestController.class))
            .map(JavaClass::reflect)
            .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
            .filter(this::isEndpoint)
            .filter(
                m ->
                    m.getGenericReturnType().getTypeName().contains(".usecase.model.") ||
                    Arrays.stream(m.getGenericParameterTypes()).anyMatch(t ->
                        t.getTypeName().contains(".usecase.model.")
                    )
            )
            .map(Method::toGenericString)
            .toList();
        assertThat(invalid).isEmpty();
    }

    @Test
    void authorizedFacadeMethodsMustReceiveAuthorizationContext() {
        var methods = classes
            .stream()
            .filter(c -> c.getPackageName().endsWith(".facade"))
            .map(JavaClass::reflect)
            .flatMap(c -> Arrays.stream(c.getMethods()))
            .filter(m -> m.isAnnotationPresent(AuthorizationRequired.class))
            .toList();
        assertThat(methods).isNotEmpty();
        assertThat(methods).allSatisfy(m ->
            assertThat(m.getParameterTypes()).as(m.toGenericString()).contains(AuthorizationContext.class)
        );
    }

    @Test
    void restEndpointsMustReceiveAuthorizationContext() {
        var methods = classes
            .stream()
            .filter(c -> c.isAnnotatedWith(RestController.class))
            .map(JavaClass::reflect)
            .flatMap(c -> Arrays.stream(c.getMethods()))
            .filter(this::isEndpoint)
            .toList();
        assertThat(methods).isNotEmpty();
        assertThat(methods).allSatisfy(m ->
            assertThat(m.getParameterTypes()).as(m.toGenericString()).contains(AuthorizationContext.class)
        );
    }

    private boolean isEndpoint(Method method) {
        return (
            method.isAnnotationPresent(RequestMapping.class) ||
            method.isAnnotationPresent(GetMapping.class) ||
            method.isAnnotationPresent(PostMapping.class) ||
            method.isAnnotationPresent(PutMapping.class) ||
            method.isAnnotationPresent(PatchMapping.class) ||
            method.isAnnotationPresent(DeleteMapping.class)
        );
    }
}
