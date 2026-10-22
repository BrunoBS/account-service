package br.com.portalmanager.platform.workspace.core.applicationconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.PublisherType;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.ResolvedPublisher;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.validation.ApplicationEnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort.PublisherReference;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.JsonSchemaValidationAdapter;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaDefinitionValidationAdapter;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import tools.jackson.databind.json.JsonMapper;

import java.util.*;

class EnvironmentConfigurationValidatorTest {
    private final PublisherReferencePort publishers = mock(PublisherReferencePort.class);
    private final SchemaSettingsValidationAdapter schemas =
            mock(SchemaSettingsValidationAdapter.class);
    private final ApplicationEnvironmentConfigurationValidator validator =
            new ApplicationEnvironmentConfigurationValidator(
                    publishers, schemas, JsonMapper.builder().build());
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String identifier = "ec766b28-5be5-43b9-a24a-ce253d6de054";

    @Test
    void acceptsApprovedCompatibilityIncludingDeprecated() {
        for (String entry :
                List.of("WEB_SOCKET,WORKSPACE", "KAFKA,WORKSPACE", "APP_CONFIG,APPLICATION")) {
            String[] values = entry.split(",");
            when(publishers.resolveForAssociation(anyCollection()))
                    .thenReturn(
                            List.of(
                                    new PublisherReference(
                                            1L,
                                            identifier,
                                            values[0],
                                            ResourceScopeTypeCode.of(values[1]),
                                            LifecycleTypeCode.active(),
                                            true)));
            assertThat(validator.validatePublishers(List.of(input(2)), Set.of())).hasSize(1);
        }
    }

    @Test
    void rejectsIncompatibleCodeOrScope() {
        for (String entry :
                List.of("WEB_SOCKET,APPLICATION", "APP_CONFIG,WORKSPACE", "OTHER,WORKSPACE")) {
            String[] values = entry.split(",");
            when(publishers.resolveForAssociation(anyCollection()))
                    .thenReturn(
                            List.of(
                                    new PublisherReference(
                                            1L,
                                            identifier,
                                            values[0],
                                            ResourceScopeTypeCode.of(values[1]),
                                            LifecycleTypeCode.active(),
                                            false)));
            assertThatThrownBy(() -> validator.validatePublishers(List.of(input(1)), Set.of()))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Test
    void inactivePublisherCanStayButCannotEnterANewAssociation() {
        when(publishers.resolveForAssociation(anyCollection()))
                .thenReturn(
                        List.of(
                                new PublisherReference(
                                        1L,
                                        identifier,
                                        "WEB_SOCKET",
                                        ResourceScopeTypeCode.workspace(),
                                        LifecycleTypeCode.inactive(),
                                        false)));
        assertThat(validator.validatePublishers(List.of(input(1)), Set.of(1L))).hasSize(1);
        assertThatThrownBy(() -> validator.validatePublishers(List.of(input(1)), Set.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void enforcesNonEmptyUniquePublishersAndPositiveDistinctOrdering() {
        assertThatThrownBy(() -> validator.validatePublishers(List.of(), Set.of()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validatePublishers(null, Set.of()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validatePublishers(List.of(input(0)), Set.of()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () -> validator.validatePublishers(List.of(input(1), input(2)), Set.of()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validatePublishers(
                                        List.of(
                                                input(1),
                                                new PublisherInput(
                                                        UUID.randomUUID().toString(),
                                                        1,
                                                        mapper.createObjectNode())),
                                        Set.of()))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(publishers);
    }

    @Test
    void resolvesOrdersAndDeepCopiesParametersAndReportsMissingPublishers() {
        String other = UUID.randomUUID().toString();
        when(publishers.resolveForAssociation(anyCollection()))
                .thenReturn(
                        List.of(
                                new PublisherReference(
                                        1L,
                                        identifier,
                                        "WEB_SOCKET",
                                        ResourceScopeTypeCode.workspace(),
                                        LifecycleTypeCode.active(),
                                        false),
                                new PublisherReference(
                                        2L,
                                        other,
                                        "KAFKA",
                                        ResourceScopeTypeCode.workspace(),
                                        LifecycleTypeCode.active(),
                                        false)));
        PublisherInput first = input(2);
        List<ResolvedPublisher> result =
                validator.validatePublishers(
                        List.of(first, new PublisherInput(other, 1, mapper.createObjectNode())),
                        Set.of());
        assertThat(result).extracting(ResolvedPublisher::sortOrder).containsExactly(1, 2);
        assertThat(result.get(1).parameters()).isNotSameAs(first.parameters());
        when(publishers.resolveForAssociation(anyCollection())).thenReturn(List.of());
        assertThatThrownBy(() -> validator.validatePublishers(List.of(input(1)), Set.of()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void enforcesCreateUpdateAndPaginationContracts() {
        validator.validateCreateVersion(null);
        validator.validateUpdateVersion(2L, 2L);
        assertThatThrownBy(() -> validator.validateCreateVersion(0L))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validateUpdateVersion(null, 2L))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validateUpdateVersion(1L, 2L))
                .isInstanceOf(ResourceVersionConflictException.class);
        validator.validatePagination(PageRequest.of(0, 20, Sort.by("createdAt")));
        assertThatThrownBy(
                        () ->
                                validator.validatePagination(
                                        PageRequest.of(0, 20, Sort.by("ownerId"))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void copyRequiresTheExplicitPublisherListToBeAbsent() {
        validator.validateCopy(true, null);
        validator.validateCopy(false, List.of(input(1)));
        assertThatThrownBy(() -> validator.validateCopy(true, List.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void mapsOnlyOfficialPublisherTypesAndSelectsDistinctBindings() {
        assertThat(PublisherType.values())
                .extracting(Enum::name)
                .containsExactly("WEB_SOCKET", "KAFKA", "APP_CONFIG");
        assertThat(Arrays.stream(PublisherType.values()).map(PublisherType::schemaCode).distinct())
                .hasSize(3);
        for (PublisherType type : PublisherType.values())
            assertThat(PublisherType.fromCode(type.name())).isEqualTo(type);
        for (String invalid : List.of("HTTP", "APPCONFIG", "WEBSOCKET", "UNKNOWN", ""))
            assertThatThrownBy(() -> PublisherType.fromCode(invalid))
                    .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> PublisherType.fromCode(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void validatesTheWholeAssociationUsingTheServerPublisherType() {
        when(publishers.resolveForAssociation(anyCollection()))
                .thenReturn(
                        List.of(
                                new PublisherReference(
                                        1L,
                                        identifier,
                                        PublisherType.KAFKA.name(),
                                        ResourceScopeTypeCode.workspace(),
                                        LifecycleTypeCode.active(),
                                        false)));
        validator.validatePublishers(List.of(input(1)), Set.of());
        org.mockito.ArgumentCaptor<tools.jackson.databind.JsonNode> payload =
                org.mockito.ArgumentCaptor.forClass(tools.jackson.databind.JsonNode.class);
        verify(schemas)
                .validateRequired(
                        eq("PLATFORM"),
                        eq(PublisherType.KAFKA.schemaCode()),
                        eq("publishers[0]"),
                        payload.capture(),
                        any());
        assertThat(payload.getValue().get("publisherIdentifier").asString()).isEqualTo(identifier);
        assertThat(payload.getValue().get("sortOrder").asInt()).isEqualTo(1);
        assertThat(payload.getValue().get("parameters").get("free").asBoolean()).isTrue();
    }

    @Test
    void validatesEmptyAndNestedParametersAndRejectsInvalidCompleteAssociations() throws Exception {
        SchemaResolutionPort resolution = mock(SchemaResolutionPort.class);
        SchemaDefinitionValidationAdapter definitions =
                new SchemaDefinitionValidationAdapter(mapper);
        SchemaSettingsValidationAdapter realSchemas =
                new SchemaSettingsValidationAdapter(
                        resolution, new JsonSchemaValidationAdapter(definitions));
        ApplicationEnvironmentConfigurationValidator real =
                new ApplicationEnvironmentConfigurationValidator(publishers, realSchemas, mapper);
        for (PublisherType type : PublisherType.values()) {
            String schema =
                    new org.springframework.core.io.ClassPathResource(
                                    "schemas/" + type.schemaCode() + ".json")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            when(resolution.resolveRequired("PLATFORM", type.schemaCode())).thenReturn(schema);
        }
        for (PublisherType type :
                List.of(PublisherType.WEB_SOCKET, PublisherType.KAFKA, PublisherType.APP_CONFIG)) {
            ResourceScopeTypeCode scope =
                    type == PublisherType.APP_CONFIG
                            ? ResourceScopeTypeCode.application()
                            : ResourceScopeTypeCode.workspace();
            when(publishers.resolveForAssociation(anyCollection()))
                    .thenReturn(
                            List.of(
                                    new PublisherReference(
                                            1L,
                                            identifier,
                                            type.name(),
                                            scope,
                                            LifecycleTypeCode.active(),
                                            false)));
            real.validatePublishers(
                    List.of(new PublisherInput(identifier, 1, mapper.createObjectNode())),
                    Set.of());
            tools.jackson.databind.JsonNode nested =
                    mapper.readTree("{\"a\":{\"b\":[true,null,123,\"text\"]}}");
            assertThat(
                            real.validatePublishers(
                                            List.of(new PublisherInput(identifier, 1, nested)),
                                            Set.of())
                                    .getFirst()
                                    .parameters())
                    .isEqualTo(nested);
            for (String invalid : List.of("null", "[]", "1", "true", "\"text\"")) {
                assertThatThrownBy(
                                () ->
                                        real.validatePublishers(
                                                List.of(
                                                        new PublisherInput(
                                                                identifier,
                                                                1,
                                                                mapper.readTree(invalid))),
                                                Set.of()))
                        .isInstanceOf(ValidationException.class);
            }
            when(publishers.resolveForAssociation(anyCollection()))
                    .thenReturn(
                            List.of(
                                    new PublisherReference(
                                            1L,
                                            "not-a-uuid",
                                            type.name(),
                                            scope,
                                            LifecycleTypeCode.active(),
                                            false)));
            assertThatThrownBy(
                            () ->
                                    real.validatePublishers(
                                            List.of(
                                                    new PublisherInput(
                                                            "not-a-uuid",
                                                            1,
                                                            mapper.createObjectNode())),
                                            Set.of()))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Test
    void doesNotSuppressSchemaResolutionOrValidationFailure() {
        when(publishers.resolveForAssociation(anyCollection()))
                .thenReturn(
                        List.of(
                                new PublisherReference(
                                        1L,
                                        identifier,
                                        PublisherType.KAFKA.name(),
                                        ResourceScopeTypeCode.workspace(),
                                        LifecycleTypeCode.active(),
                                        false)));
        doThrow(new ValidationException("schema.unavailable"))
                .when(schemas)
                .validateRequired(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(tools.jackson.databind.JsonNode.class),
                        any());
        assertThatThrownBy(() -> validator.validatePublishers(List.of(input(1)), Set.of()))
                .isInstanceOf(ValidationException.class);
    }

    private PublisherInput input(int order) {
        return new PublisherInput(identifier, order, mapper.createObjectNode().put("free", true));
    }
}
