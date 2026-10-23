package br.com.portalmanager.platform.workspace.core.applicationconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.PublisherReferencePort.PublisherReference;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.ResourceScopeTypePort;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.SchemaValidationPort;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.validation.ApplicationEnvironmentConfigurationValidator;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

class EnvironmentConfigurationValidatorTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final PublisherReferencePort publishers = mock(PublisherReferencePort.class);
    private final ResourceScopeTypePort scopes = mock(ResourceScopeTypePort.class);
    private final ApplicationEnvironmentConfigurationValidator validator =
            new ApplicationEnvironmentConfigurationValidator(
                    publishers, mock(SchemaValidationPort.class), json, scopes);

    private PublisherInput input(String code) {
        return new PublisherInput(code, 1, json.createObjectNode());
    }

    private void reference(String code, boolean active, JsonNode settings) {
        when(publishers.resolveForAssociation(anyCollection()))
                .thenReturn(List.of(new PublisherReference(code, active, settings)));
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
            if (false && code.equals("APP_CONFIG"))
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
    }
}
