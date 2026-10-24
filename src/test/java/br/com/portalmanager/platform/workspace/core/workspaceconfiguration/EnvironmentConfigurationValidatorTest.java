package br.com.portalmanager.platform.workspace.core.workspaceconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.ResolvedPublisher;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.PublisherReferencePort.PublisherReference;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.ResourceScopeTypePort;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.SchemaValidationPort;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.validation.WorkspaceEnvironmentConfigurationValidator;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

class EnvironmentConfigurationValidatorTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final PublisherReferencePort publishers = mock(PublisherReferencePort.class);
    private final ResourceScopeTypePort scopes = mock(ResourceScopeTypePort.class);
    private final SchemaValidationPort schemas = mock(SchemaValidationPort.class);
    private final WorkspaceEnvironmentConfigurationValidator validator =
            new WorkspaceEnvironmentConfigurationValidator(
                    publishers, schemas, scopes);

    private PublisherInput input(String code) {
        return new PublisherInput(code, 1, json.createObjectNode());
    }

    private void reference(String code, boolean active, JsonNode settings) {
        when(publishers.resolveForAssociation(anyCollection()))
                .thenReturn(List.of(new PublisherReference(code, active, settings)));
    }

    @Test
    void acceptsExplicitNullSettingsWithoutRunningObjectSchemaValidation() throws Exception {
        validator.validateSettings(json.readTree("null"));

        verifyNoInteractions(schemas);
    }

    @Test
    void validatesNonNullSettingsAgainstTheConfiguredSchema() {
        var settings = json.createObjectNode().put("region", "test");
        validator.validateSettings(settings);
        verify(schemas).validate(eq("PLATFORM"), eq("workspace-environment-configuration-settings"), eq("settings"),
                same(settings), any());
    }

    @Test
    void sendsAbsentJavaSettingsToTheSchemaAdapter() {
        validator.validateSettings(null);
        verify(schemas).validate(eq("PLATFORM"), eq("workspace-environment-configuration-settings"),
                eq("settings"), isNull(), any());
    }

    @Test
    void validatesTheThreeCatalogCodesAndTheirInitialScopes() {
        when(scopes.existsActive(anyString())).thenReturn(true);
        for (String code : List.of("WEB_SOCKET", "KAFKA", "APP_CONFIG")) {
            reference(
                    code,
                    true,
                    json.createObjectNode()
                            .put("scope", code.equals("APP_CONFIG") ? "APPLICATION" : "WORKSPACE"));
            if (code.equals("APP_CONFIG"))
                assertThatThrownBy(
                                () -> validator.validatePublishers(List.of(input(code)), Set.of()))
                        .isInstanceOf(ValidationException.class);
            else
                assertThat(
                                validator
                                        .validatePublishers(List.of(input(code)), Set.of())
                                        .getFirst()
                                        .publisherCode())
                        .isEqualTo(code);
        }
    }

    @Test
    void rejectsMissingPublishersAndMalformedOrMissingScopeAsNotConfigured() throws Exception {
        when(publishers.resolveForAssociation(anyCollection())).thenReturn(List.of());
        assertThatThrownBy(() -> validator.validatePublishers(List.of(input("KAFKA")), Set.of()))
                .isInstanceOf(ValidationException.class);
        for (String settings :
                List.of(
                        "{}",
                        "[]",
                        "null",
                        "{\"scope\":1}",
                        "{\"scope\":null}",
                        "{\"scope\":\"UNKNOWN\"}")) {
            reference("KAFKA", true, json.readTree(settings));
            assertThatThrownBy(
                            () -> validator.validatePublishers(List.of(input("KAFKA")), Set.of()))
                    .isInstanceOf(ValidationException.class);
        }
        reference("KAFKA", true, json.createObjectNode().put("scope", "WORKSPACE"));
        when(scopes.existsActive("WORKSPACE")).thenReturn(false);
        assertThatThrownBy(() -> validator.validatePublishers(List.of(input("KAFKA")), Set.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void retainsInactiveExistingButRejectsNewAndCopyAssociations() {
        when(scopes.existsActive("WORKSPACE")).thenReturn(true);
        reference("KAFKA", false, json.createObjectNode().put("scope", "WORKSPACE"));
        assertThat(validator.validatePublishers(List.of(input("KAFKA")), Set.of("KAFKA")))
                .hasSize(1);
        assertThatThrownBy(() -> validator.validatePublishers(List.of(input("KAFKA")), Set.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void enforcesListUniquenessAndPositiveDistinctOrder() {
        assertThatThrownBy(() -> validator.validatePublishers(null, Set.of()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validatePublishers(List.of(), Set.of()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validatePublishers(
                                        List.of(input("KAFKA"), input("KAFKA")), Set.of()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validatePublishers(
                                        List.of(input("KAFKA"), input("WEB_SOCKET")), Set.of()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validatePublishers(
                                        List.of(
                                                new PublisherInput(
                                                        "KAFKA", 0, json.createObjectNode())),
                                        Set.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void preservesOpenParametersAndRejectsNonObjects() throws Exception {
        when(scopes.existsActive("WORKSPACE")).thenReturn(true);
        reference("KAFKA", true, json.createObjectNode().put("scope", "WORKSPACE"));
        JsonNode parameters =
                json.readTree("{\"nested\":{\"arbitrary\":[true,null,123,\"text\"]}}");
        assertThat(
                        validator
                                .validatePublishers(
                                        List.of(new PublisherInput("KAFKA", 1, parameters)),
                                        Set.of())
                                .getFirst()
                                .parameters())
                .isEqualTo(parameters)
                .isNotSameAs(parameters);
        for (String invalid : List.of("null", "[]", "1", "true", "\"text\"")) {
            assertThatThrownBy(
                            () ->
                                    validator.validatePublishers(
                                            List.of(
                                                    new PublisherInput(
                                                            "KAFKA", 1, json.readTree(invalid))),
                                            Set.of()))
                    .isInstanceOf(ValidationException.class);
        }
        assertThatThrownBy(() -> validator.validatePublishers(
                List.of(new PublisherInput("KAFKA", 1, null)), Set.of()))
                .isInstanceOf(ValidationException.class);
        reference("KAFKA", true, null);
        assertThatThrownBy(() -> validator.validatePublishers(List.of(input("KAFKA")), Set.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void preservesVersionAndPaginationRules() {
        validator.validateCreateVersion(null);
        validator.validateUpdateVersion(1L, 1L);
        assertThatThrownBy(() -> validator.validateCreateVersion(0L))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validateUpdateVersion(null, 1L))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validateUpdateVersion(0L, 1L))
                .isInstanceOf(ResourceVersionConflictException.class);
        assertThatThrownBy(() -> validator.validateUpdateVersion(-1L, -1L))
                .isInstanceOf(ValidationException.class);
        validator.validatePagination(PageRequest.of(0, 20, Sort.by("createdAt")));
        validator.validatePagination(PageRequest.of(0, 20, Sort.by("id")));
        for (Pageable invalid : List.of(Pageable.unpaged(), PageRequest.of(0, 20, Sort.by("settings")),
                PageRequest.of(Integer.MAX_VALUE, 2))) {
            assertThatThrownBy(() -> validator.validatePagination(invalid)).isInstanceOf(ValidationException.class);
        }
        assertThatThrownBy(() -> validator.validatePagination(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullPublisherFieldsAndSortOrders() {
        for (PublisherInput invalid : List.of(new PublisherInput(null, 1, json.createObjectNode()),
                new PublisherInput("KAFKA", null, json.createObjectNode()),
                new PublisherInput("KAFKA", -1, json.createObjectNode()))) {
            assertThatThrownBy(() -> validator.validatePublishers(List.of(invalid), Set.of()))
                    .isInstanceOf(ValidationException.class);
        }
        assertThatThrownBy(() -> validator.validatePublishers(java.util.Arrays.asList((PublisherInput) null), Set.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsSettingsWhenBoundSchemaReturnsValidationErrors() {
        doAnswer(invocation -> {
            ((br.com.portalmanager.platform.library.messaging.validation.ValidationResult) invocation.getArgument(4))
                    .addError("settings.region", br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys.VALIDATION_FAILED);
            return null;
        }).when(schemas).validate(anyString(), anyString(), anyString(), any(), any());
        assertThatThrownBy(() -> validator.validateSettings(json.createObjectNode()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void sortsMultipleConfiguredPublishersByRequestedOrder() {
        when(scopes.existsActive("WORKSPACE")).thenReturn(true);
        when(publishers.resolveForAssociation(anyCollection())).thenReturn(List.of(
                new PublisherReference("WEB_SOCKET", true, json.createObjectNode().put("scope", "WORKSPACE")),
                new PublisherReference("KAFKA", true, json.createObjectNode().put("scope", "WORKSPACE"))));
        var resolved = validator.validatePublishers(List.of(
                new PublisherInput("WEB_SOCKET", 2, json.createObjectNode()),
                new PublisherInput("KAFKA", 1, json.createObjectNode())), Set.of());
        assertThat(resolved).extracting(ResolvedPublisher::publisherCode)
                .containsExactly("KAFKA", "WEB_SOCKET");
    }
}
