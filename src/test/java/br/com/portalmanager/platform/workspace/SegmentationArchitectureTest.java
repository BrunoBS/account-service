package br.com.portalmanager.platform.workspace;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.CategoryController;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.SegmentController;
import br.com.portalmanager.platform.workspace.feature.segmentation.facade.category.CategoryFacade;
import br.com.portalmanager.platform.workspace.feature.segmentation.facade.segment.SegmentFacade;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

class SegmentationArchitectureTest {
    @Test
    void segmentCatalogMustNotDependOnOtherDomainModulesOrTheirRepositories() {
        noClasses()
                .that()
                .resideInAPackage("..feature.segmentation..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..core..",
                        "..feature.servicecontext..",
                        "..feature.sharing..",
                        "..feature.dynamicbundle..",
                        "..feature.onboarding..")
                .check(
                        new ClassFileImporter()
                                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                                .importPackages("br.com.portalmanager.platform.workspace"));
    }

    @Test
    void everyFacadeOperationHasTheExplicitOwnerOrAuthenticatedReadPolicy() {
        for (Class<?> facade : List.of(SegmentFacade.class, CategoryFacade.class)) {
            for (Method method : facade.getDeclaredMethods()) {
                if (method.isSynthetic()) continue;
                AuthorizationRequired policy = method.getAnnotation(AuthorizationRequired.class);
                assertThat(policy).as(method.toGenericString()).isNotNull();
                assertThat(policy.level())
                        .isEqualTo(
                                policy.action() == AuthorizationAction.READ
                                        ? AuthorizationLevel.OPEN
                                        : AuthorizationLevel.OWNER);
            }
        }
    }

    @Test
    void everyCategoryEndpointGetsItsSegmentFromThePathAndNeverFromTheBody() {
        assertThat(CategoryController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/v1/segmentation/segments/{segmentIdentifier}/categories");
        for (Method method : CategoryController.class.getDeclaredMethods()) {
            if (method.isSynthetic()) continue;
            assertThat(
                            Arrays.stream(method.getParameters())
                                    .filter(
                                            parameter ->
                                                    parameter
                                                            .getName()
                                                            .equals("segmentIdentifier")))
                    .singleElement()
                    .satisfies(
                            parameter -> {
                                assertThat(parameter.getType()).isEqualTo(java.util.UUID.class);
                                assertThat(parameter.isAnnotationPresent(PathVariable.class))
                                        .isTrue();
                            });
            Arrays.stream(method.getParameters())
                    .filter(parameter -> parameter.isAnnotationPresent(RequestBody.class))
                    .forEach(
                            parameter ->
                                    assertThat(
                                                    Arrays.stream(
                                                                    parameter
                                                                            .getType()
                                                                            .getRecordComponents())
                                                            .map(component -> component.getName()))
                                            .doesNotContain("segmentIdentifier"));
        }
    }

    @Test
    void controllersExposeSixOperationsWithoutDeleteAndValidateTypedRequestsViaTheLibrary() {
        for (Class<?> controller : List.of(SegmentController.class, CategoryController.class)) {
            assertThat(
                            Arrays.stream(controller.getDeclaredMethods())
                                    .filter(method -> !method.isSynthetic()))
                    .hasSize(6);
            assertThat(
                            Arrays.stream(controller.getDeclaredMethods())
                                    .filter(m -> m.isAnnotationPresent(DeleteMapping.class)))
                    .isEmpty();
            for (Method method : controller.getDeclaredMethods()) {
                Arrays.stream(method.getParameters())
                        .filter(p -> p.isAnnotationPresent(RequestBody.class))
                        .forEach(
                                parameter -> {
                                    assertThat(parameter.getType().isRecord()).isTrue();
                                    assertThat(
                                                    parameter
                                                            .getType()
                                                            .isAnnotationPresent(
                                                                    ValidateResourceSchemaRequest
                                                                            .class))
                                            .isTrue();
                                });
            }
        }
    }
}
