package br.com.portalmanager.platform.workspace.unit.core.workspaceconfiguration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfigurationPublisher;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.mapping.WorkspaceEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.operations.*;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.EnvironmentReferencePort;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.PhaseCompletionPort;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.validation.WorkspaceEnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherTypeCode;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

class EnvironmentConfigurationCommandTest {
    private final WorkspaceEnvironmentConfigurationRepository repository = mock(WorkspaceEnvironmentConfigurationRepository.class);
    private final WorkspaceEnvironmentConfigurationPublisherRepository associations = mock(WorkspaceEnvironmentConfigurationPublisherRepository.class);
    private final WorkspaceEnvironmentConfigurationScope scopes = mock(WorkspaceEnvironmentConfigurationScope.class);
    private final EnvironmentReferencePort environments = mock(EnvironmentReferencePort.class);
    private final WorkspaceEnvironmentConfigurationValidator validator = mock(WorkspaceEnvironmentConfigurationValidator.class);
    private final WorkspaceEnvironmentConfigurationMapper mapper = mock(WorkspaceEnvironmentConfigurationMapper.class);
    private final PhaseCompletionPort phases = mock(PhaseCompletionPort.class);
    private final WorkspaceEnvironmentConfigurationCommandService commands =
            new WorkspaceEnvironmentConfigurationCommandService(repository, associations, scopes,
                    environments, validator, mapper, phases);
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void createsConfigurationAndRegistersWorkspaceFirstEnvironment() {
        setupScope();
        var settings = json.createObjectNode().put("region", "test");
        var selected = List.of(new ResolvedPublisher("KAFKA", 1, json.createObjectNode().put("topicName", "events")));
        when(validator.validatePublishers(anyList(), eq(Set.of()))).thenReturn(selected);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        commands.create("workspace", new CreateWorkspaceEnvironmentConfigurationInput(null, "environment", settings,
                List.of(new PublisherInput("KAFKA", 1, json.createObjectNode().put("topicName", "events")))));

        ArgumentCaptor<WorkspaceEnvironmentConfiguration> configuration = ArgumentCaptor.forClass(WorkspaceEnvironmentConfiguration.class);
        verify(repository).saveAndFlush(configuration.capture());
        assertThat(configuration.getValue().getWorkspaceId()).isEqualTo(10L);
        assertThat(configuration.getValue().getEnvironmentId()).isEqualTo(20L);
        verify(associations).saveAllAndFlush(anyList());
        verify(phases).registerPhaseCompletion(eq(10L), any());
    }

    @Test
    void updateReplacesPublishersAndCopiesSettingsUsingCurrentVersion() {
        var current = stored();
        var existing = new WorkspaceEnvironmentConfigurationPublisher(current, PublisherTypeCode.kafka(), 1,
                json.createObjectNode().put("topicName", "old"));
        when(associations.findByConfigurationIdOrderBySortOrderAsc(30L)).thenReturn(List.of(existing));
        when(validator.validatePublishers(anyList(), eq(Set.of("KAFKA"))))
                .thenReturn(List.of(new ResolvedPublisher("KAFKA", 2, json.createObjectNode().put("topicName", "new"))));
        when(environments.resolveIdentifier(20L)).thenReturn("environment");
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        commands.update("workspace", current.getIdentifier(), new UpdateWorkspaceEnvironmentConfigurationInput(
                4L, json.createObjectNode().put("region", "updated"),
                List.of(new PublisherInput("KAFKA", 2, json.createObjectNode().put("topicName", "new")))));

        verify(environments).resolveActiveForUpdate(10L, "environment");
        verify(associations).deleteAssociations(30L);
        verify(associations).saveAllAndFlush(anyList());
        assertThat(current.getSettings().get("region").asString()).isEqualTo("updated");
    }

    @Test
    void inactivatesConfigurationAndPersistsIt() {
        var current = stored();
        commands.inactivate("workspace", current.getIdentifier());
        assertThat(current.getLifecycle().value()).isEqualTo("INACTIVE");
        verify(repository).saveAndFlush(current);
    }

    @Test
    void quarantinesInactiveConfigurationAndPersistsIt() {
        var current = stored();
        current.inactivate(LocalDateTime.now());
        commands.quarantine("workspace", current.getIdentifier());
        assertThat(current.getLifecycle().value()).isEqualTo("QUARANTINED");
        verify(repository).saveAndFlush(current);
    }

    @Test
    void missingConfigurationIsNotFoundForUpdateAndLifecycleCommands() {
        when(scopes.resolve("workspace", true)).thenReturn(new WorkspaceEnvironmentConfigurationScope.Scope(10L, 10L));
        when(repository.findByIdentifierAndWorkspaceId(anyString(), eq(10L))).thenReturn(Optional.empty());
        var update = new UpdateWorkspaceEnvironmentConfigurationInput(1L, json.createObjectNode(), List.of());

        assertThatThrownBy(() -> commands.update("workspace", "missing", update)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commands.inactivate("workspace", "missing")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commands.quarantine("workspace", "missing")).isInstanceOf(NotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectedPublisherDoesNotPersistConfigurationOrReplaceAssociations() {
        var current = stored();
        when(environments.resolveIdentifier(20L)).thenReturn("environment");
        doThrow(new ValidationException(PlatformMessageKeys.VALIDATION_FAILED))
                .when(validator).validatePublishers(anyList(), anySet());
        assertThatThrownBy(() -> commands.update("workspace", current.getIdentifier(),
                new UpdateWorkspaceEnvironmentConfigurationInput(4L, json.createObjectNode(),
                        List.of(new PublisherInput("UNKNOWN", 1, json.createObjectNode())))))
                .isInstanceOf(ValidationException.class);
        verify(repository, never()).saveAndFlush(any());
        verify(associations, never()).deleteAssociations(any());
    }

    private void setupScope() {
        when(scopes.resolve("workspace", true)).thenReturn(new WorkspaceEnvironmentConfigurationScope.Scope(10L, 10L));
        when(environments.resolveActiveForUpdate(10L, "environment")).thenReturn(20L);
    }

    private WorkspaceEnvironmentConfiguration stored() {
        setupScope();
        var configuration = new WorkspaceEnvironmentConfiguration(10L, 20L, json.createObjectNode().put("region", "before"), LocalDateTime.now());
        ReflectionTestUtils.setField(configuration, "id", 30L);
        ReflectionTestUtils.setField(configuration, "version", 4L);
        when(repository.findByIdentifierAndWorkspaceId(configuration.getIdentifier(), 10L)).thenReturn(Optional.of(configuration));
        return configuration;
    }
}
