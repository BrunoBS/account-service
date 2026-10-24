package br.com.portalmanager.platform.workspace.core.applicationconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.mapping.ApplicationEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.operations.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.EnvironmentReferencePort;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.PhaseCompletionPort;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.WorkspaceEnvironmentConfigurationPort;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.WorkspaceEnvironmentConfigurationPort.PublisherCopy;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.validation.ApplicationEnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherTypeCode;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.*;

class EnvironmentConfigurationCommandTest {
    private final ApplicationEnvironmentConfigurationRepository repository =
            mock(ApplicationEnvironmentConfigurationRepository.class);
    private final ApplicationEnvironmentConfigurationPublisherRepository associations =
            mock(ApplicationEnvironmentConfigurationPublisherRepository.class);
    private final ApplicationEnvironmentConfigurationScope scopes =
            mock(ApplicationEnvironmentConfigurationScope.class);
    private final EnvironmentReferencePort environments = mock(EnvironmentReferencePort.class);
    private final ApplicationEnvironmentConfigurationValidator validator =
            mock(ApplicationEnvironmentConfigurationValidator.class);
    private final ApplicationEnvironmentConfigurationMapper mapper =
            mock(ApplicationEnvironmentConfigurationMapper.class);
    private final WorkspaceEnvironmentConfigurationPort origin =
            mock(WorkspaceEnvironmentConfigurationPort.class);
    private final ApplicationEnvironmentConfigurationCommandService commands =
            new ApplicationEnvironmentConfigurationCommandService(
                    repository,
                    associations,
                    scopes,
                    environments,
                    validator,
                    mapper,
                    origin,
                    mock(PhaseCompletionPort.class));
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void createCopiesPublisherSnapshotAndKeepsTheApplicationSettings() {
        setupScope();
        ObjectNode parameters = json.createObjectNode().put("topic", "source");
        ObjectNode settings = json.createObjectNode().put("application", true);
        when(origin.copyPublishers(1L, 3L))
                .thenReturn(
                        List.of(
                                new PublisherCopy(
                                        PublisherTypeCode.kafka().value(), 4, parameters)));
        when(validator.validatePublishers(anyList(), eq(Set.of())))
                .thenReturn(
                        List.of(
                                new ResolvedPublisher(
                                        PublisherTypeCode.kafka().value(), 4, parameters)));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        commands.create(
                "workspace",
                "application",
                new CreateApplicationEnvironmentConfigurationInput(
                        null, "environment", settings, null, true));
        ArgumentCaptor<ApplicationEnvironmentConfiguration> captured =
                ArgumentCaptor.forClass(ApplicationEnvironmentConfiguration.class);
        verify(repository).saveAndFlush(captured.capture());
        assertThat(captured.getValue().getSettings()).isEqualTo(settings);
        assertThat(captured.getValue().getApplicationId()).isEqualTo(2L);
        assertThat(captured.getValue().getEnvironmentId()).isEqualTo(3L);
        verify(origin).copyPublishers(1L, 3L);
        verify(validator)
                .validatePublishers(
                        List.of(
                                new PublisherInput(
                                        PublisherTypeCode.kafka().value(), 4, parameters)),
                        Set.of());
    }

    @Test
    void explicitPublishersStillRequireWorkspaceConfigurationButAreNotReplacedByItsPublishers() {
        setupScope();
        List<PublisherInput> explicit =
                List.of(
                        new PublisherInput(
                                PublisherTypeCode.kafka().value(), 1, json.createObjectNode()));
        when(origin.copyPublishers(1L, 3L)).thenReturn(List.of());
        when(validator.validatePublishers(explicit, Set.of())).thenReturn(List.of());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        commands.create(
                "workspace",
                "application",
                new CreateApplicationEnvironmentConfigurationInput(
                        null, "environment", json.createObjectNode(), explicit, false));
        verify(origin).copyPublishers(1L, 3L);
        verify(validator).validatePublishers(explicit, Set.of());
    }

    @Test
    void staleVersionStopsBeforeLookingUpOrReplacingTheOrigin() {
        ApplicationEnvironmentConfiguration configuration = stored();
        doThrow(new ResourceVersionConflictException())
                .when(validator)
                .validateUpdateVersion(0L, 4L);
        assertThatThrownBy(
                        () ->
                                commands.update(
                                        "workspace",
                                        "application",
                                        configuration.getIdentifier(),
                                        new UpdateApplicationEnvironmentConfigurationInput(
                                                0L, json.createObjectNode(), null, true)))
                .isInstanceOf(ResourceVersionConflictException.class);
        verifyNoInteractions(origin, associations);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void recopyMustValidateAsNewEvenWhenThePublisherAlreadyExistsAtTheDestination() {
        ApplicationEnvironmentConfiguration configuration = stored();
        ObjectNode parameters = json.createObjectNode();
        when(environments.resolveIdentifier(3L)).thenReturn("environment");
        when(origin.copyPublishers(1L, 3L))
                .thenReturn(
                        List.of(
                                new PublisherCopy(
                                        PublisherTypeCode.kafka().value(), 1, parameters)));
        when(associations.findByConfigurationIdOrderBySortOrderAsc(9L))
                .thenReturn(
                        List.of(
                                new ApplicationEnvironmentConfigurationPublisher(
                                        configuration, PublisherTypeCode.kafka(), 1, parameters)));
        when(validator.validatePublishers(anyList(), eq(Set.of())))
                .thenThrow(new ValidationException(PlatformMessageKeys.VALIDATION_FAILED));
        assertThatThrownBy(
                        () ->
                                commands.update(
                                        "workspace",
                                        "application",
                                        configuration.getIdentifier(),
                                        new UpdateApplicationEnvironmentConfigurationInput(
                                                4L, json.createObjectNode(), null, true)))
                .isInstanceOf(ValidationException.class);
        verify(associations, never()).deleteAssociations(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateCanReplacePublishersFromWorkspaceAndValidatesUpdatedRequestShape() {
        ApplicationEnvironmentConfiguration configuration = stored();
        ObjectNode parameters = json.createObjectNode().put("topicName", "copied");
        when(environments.resolveIdentifier(3L)).thenReturn("environment");
        when(origin.copyPublishers(1L, 3L)).thenReturn(List.of(
                new PublisherCopy(PublisherTypeCode.kafka().value(), 1, parameters)));
        when(validator.validatePublishers(anyList(), eq(Set.of()))).thenReturn(List.of(
                new ResolvedPublisher(PublisherTypeCode.kafka().value(), 1, parameters)));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        commands.update("workspace", "application", configuration.getIdentifier(),
                new UpdateApplicationEnvironmentConfigurationInput(4L, json.createObjectNode(), null, true));

        verify(origin).copyPublishers(1L, 3L);
        verify(validator).validateCopiedRequest(isNull(), eq(4L), any(), anyList());
        verify(associations, never()).findByConfigurationIdOrderBySortOrderAsc(any());
        verify(associations).deleteAssociations(9L);
    }

    private void setupScope() {
        when(scopes.resolve("workspace", "application", true))
                .thenReturn(new ApplicationEnvironmentConfigurationScope.Scope(1L, 2L));
        when(environments.resolveActiveForUpdate(1L, "environment")).thenReturn(3L);
    }

    private ApplicationEnvironmentConfiguration stored() {
        setupScope();
        ApplicationEnvironmentConfiguration configuration =
                new ApplicationEnvironmentConfiguration(
                        2L, 3L, json.createObjectNode(), LocalDateTime.now());
        ReflectionTestUtils.setField(configuration, "id", 9L);
        ReflectionTestUtils.setField(configuration, "version", 4L);
        when(repository.findByIdentifierAndApplicationId(configuration.getIdentifier(), 2L))
                .thenReturn(Optional.of(configuration));
        return configuration;
    }
}
