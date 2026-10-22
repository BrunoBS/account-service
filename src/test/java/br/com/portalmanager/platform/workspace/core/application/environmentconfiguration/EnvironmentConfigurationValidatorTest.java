package br.com.portalmanager.platform.workspace.core.application.environmentconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.model.ResolvedPublisher;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.validation.ApplicationEnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort.PublisherReference;
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
            new ApplicationEnvironmentConfigurationValidator(publishers, schemas);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String identifier = "ec766b28-5be5-43b9-a24a-ce253d6de054";

    @Test
    void acceptsApprovedCompatibilityIncludingDeprecated() {
        for (String entry : List.of("HTTP,WORKSPACE", "KAFKA,WORKSPACE", "APPCONFIG,APPLICATION")) {
            String[] values = entry.split(",");
            when(publishers.resolveForAssociation(anyCollection()))
                    .thenReturn(
                            List.of(
                                    new PublisherReference(
                                            1L, identifier, values[0], values[1], "ACTIVE", true)));
            assertThat(validator.validatePublishers(List.of(input(2)), Set.of())).hasSize(1);
        }
    }

    @Test
    void rejectsIncompatibleCodeOrScope() {
        for (String entry : List.of("HTTP,APPLICATION", "APPCONFIG,WORKSPACE", "OTHER,WORKSPACE")) {
            String[] values = entry.split(",");
            when(publishers.resolveForAssociation(anyCollection()))
                    .thenReturn(
                            List.of(
                                    new PublisherReference(
                                            1L,
                                            identifier,
                                            values[0],
                                            values[1],
                                            "ACTIVE",
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
                                        1L, identifier, "HTTP", "WORKSPACE", "INACTIVE", false)));
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
                                        1L, identifier, "HTTP", "WORKSPACE", "ACTIVE", false),
                                new PublisherReference(
                                        2L, other, "KAFKA", "WORKSPACE", "ACTIVE", false)));
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

    private PublisherInput input(int order) {
        return new PublisherInput(identifier, order, mapper.createObjectNode().put("free", true));
    }
}
