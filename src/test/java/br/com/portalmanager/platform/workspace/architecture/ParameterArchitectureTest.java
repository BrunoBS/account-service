package br.com.portalmanager.platform.workspace.architecture;


import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.ParameterKeyController;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.CreateParameterKeyRequest;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.UpdateParameterKeyRequest;
import br.com.portalmanager.platform.workspace.feature.parameter.facade.DynamicParameterFacade;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PatchMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

class ParameterArchitectureTest {
    @Test
    void dynamicParameterUsesPublicUseCasesRatherThanRepositoriesOrEntitiesOfOtherModules() {
        noClasses()
                .that()
                .resideInAPackage("..feature.parameter..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..core..repository..",
                        "..feature.servicecontext.repository..",
                        "..feature.servicecontext.domain..",
                        "..feature.sharing..",
                        "..feature.dynamicbundle..")
                .check(
                        new ClassFileImporter()
                                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                                .importPackages("br.com.portalmanager.platform.workspace"));
    }

    @Test
    void dynamicParameterDelegatesRedisInfrastructureToTheLibrary() {
        noClasses()
                .that()
                .resideInAPackage("..feature.parameter..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.data.redis..", "io.lettuce..", "redis.clients..")
                .check(
                        new ClassFileImporter()
                                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                                .importPackages("br.com.portalmanager.platform.workspace"));
    }

    @Test
    void publicationUseCasesDependOnTheBusinessPortInsteadOfRedis() {
        noClasses()
                .that()
                .resideInAPackage("..feature.parameter.usecase..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("br.com.portalmanager.platform.library.redis..")
                .check(new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("br.com.portalmanager.platform.workspace"));
    }

    @Test
    void everyAdministrativeOperationRequiresOwnerAndPublicationIsExplicit() {
        for (Method method : DynamicParameterFacade.class.getDeclaredMethods()) {
            if (method.isSynthetic()) continue;
            assertThat(method.getAnnotation(AuthorizationRequired.class)).isNotNull();
            assertThat(method.getAnnotation(AuthorizationRequired.class).level())
                    .isEqualTo(AuthorizationLevel.OWNER);
        }
        assertThat(
                        Arrays.stream(ParameterKeyController.class.getDeclaredMethods())
                                .filter(method -> !method.isSynthetic()))
                .hasSize(10);
        assertThat(
                        Arrays.stream(ParameterKeyController.class.getDeclaredMethods())
                                .filter(method -> method.isAnnotationPresent(PatchMapping.class))
                                .flatMap(
                                        method ->
                                                Arrays.stream(
                                                        method.getAnnotation(PatchMapping.class)
                                                                .value())))
                .containsExactlyInAnyOrder("/{identifier}/activate", "/{identifier}/inactivate");
    }

    @Test
    void cacheAdministrationHasExplicitOwnerReadAndDeletePolicies() throws Exception {
        Method read = DynamicParameterFacade.class.getMethod("findCache",
                br.com.portalmanager.platform.library.authorization.model.AuthorizationContext.class,
                String.class, String.class, Integer.class);
        Method clear = DynamicParameterFacade.class.getMethod("clearCache",
                br.com.portalmanager.platform.library.authorization.model.AuthorizationContext.class, String.class);
        assertThat(read.getAnnotation(AuthorizationRequired.class).action())
                .isEqualTo(br.com.portalmanager.platform.library.authorization.model.AuthorizationAction.READ);
        assertThat(clear.getAnnotation(AuthorizationRequired.class).action())
                .isEqualTo(br.com.portalmanager.platform.library.authorization.model.AuthorizationAction.DELETE);
        assertThat(ParameterKeyController.class.getMethod("findCache",
                br.com.portalmanager.platform.library.authorization.model.AuthorizationContext.class,
                java.util.UUID.class, String.class, Integer.class)
                .getAnnotation(org.springframework.web.bind.annotation.GetMapping.class).value()).containsExactly("/cache");
        assertThat(ParameterKeyController.class.getMethod("clearCache",
                br.com.portalmanager.platform.library.authorization.model.AuthorizationContext.class, java.util.UUID.class)
                .getAnnotation(org.springframework.web.bind.annotation.DeleteMapping.class).value()).containsExactly("/cache");
    }

    @Test
    void publicRoutesAndEntityUseOnlyTheNewDomainNames() {
        assertThat(
                        ParameterKeyController.class
                                .getAnnotation(
                                        org.springframework.web.bind.annotation.RequestMapping
                                                .class)
                                .value())
                .containsExactly("/api/v1/dynamic-parameters");
        assertThat(
                        br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKey.class
                                .getAnnotation(jakarta.persistence.Table.class)
                                .name())
                .isEqualTo("parameter_keys");
        assertThat(
                        br.com.portalmanager.platform.workspace.entrypoint.catalog
                                .ParameterKeyTypeController.class
                                .getAnnotation(
                                        org.springframework.web.bind.annotation.RequestMapping
                                                .class)
                                .value())
                .containsExactly("/api/v1/parameter-key-type");
        assertThat(
                        br.com.portalmanager.platform.workspace.entrypoint.catalog
                                .ParameterKeyStatusTypeController.class
                                .getAnnotation(
                                        org.springframework.web.bind.annotation.RequestMapping
                                                .class)
                                .value())
                .containsExactly("/api/v1/parameter-key-status-type");
    }

    @Test
    void typedRequestsUseTheLibraryAndCommandSnapshotsContainNoValuesOrNumericIds() {
        for (Class<?> request :
                List.of(CreateParameterKeyRequest.class, UpdateParameterKeyRequest.class)) {
            assertThat(request.isRecord()).isTrue();
            assertThat(request.isAnnotationPresent(ValidateResourceSchemaRequest.class)).isTrue();
            assertThat(request.getAnnotation(ValidateResourceSchemaRequest.class).type())
                    .isEqualTo("PLATFORM");
            assertThat(request.getAnnotation(ValidateResourceSchemaRequest.class).code())
                    .isEqualTo("dynamic-parameter");
        }
        assertThat(
                        Arrays.stream(UpdateParameterKeyRequest.class.getRecordComponents())
                                .map(component -> component.getName()))
                .containsExactly("version", "description", "type", "value");
        assertThat(
                        Arrays.stream(ParameterKeyMetadata.class.getRecordComponents())
                                .map(component -> component.getName()))
                .doesNotContain("id", "microserviceId", "value", "publishedValue", "publishedType");
    }
}
