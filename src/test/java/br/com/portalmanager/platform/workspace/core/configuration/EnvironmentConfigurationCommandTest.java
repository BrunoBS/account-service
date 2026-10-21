package br.com.portalmanager.platform.workspace.core.configuration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.configuration.application.domain.*;
import br.com.portalmanager.platform.workspace.core.configuration.application.repository.*;
import br.com.portalmanager.platform.workspace.core.configuration.application.usecase.mapping.ApplicationEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.configuration.application.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.configuration.application.usecase.operations.*;
import br.com.portalmanager.platform.workspace.core.configuration.model.*;
import br.com.portalmanager.platform.workspace.core.configuration.validation.EnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.EnvironmentReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceEnvironmentConfigurationPort;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceEnvironmentConfigurationPort.PublisherCopy;

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
    private final EnvironmentConfigurationValidator validator =
            mock(EnvironmentConfigurationValidator.class);
    private final ApplicationEnvironmentConfigurationMapper mapper =
            mock(ApplicationEnvironmentConfigurationMapper.class);
    private final WorkspaceEnvironmentConfigurationPort origin =
            mock(WorkspaceEnvironmentConfigurationPort.class);
    private final ApplicationEnvironmentConfigurationCommandService commands =
            new ApplicationEnvironmentConfigurationCommandService(
                    repository, associations, scopes, environments, validator, mapper, origin);
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void createCopiesPublisherSnapshotAndKeepsTheApplicationSettings() {
        setupScope();
        ObjectNode parameters = json.createObjectNode().put("topic", "source");
        ObjectNode settings = json.createObjectNode().put("application", true);
        when(origin.copyPublishers(1L, 3L))
                .thenReturn(List.of(new PublisherCopy("publisher", 4, parameters)));
        when(validator.validatePublishers(eq(true), anyList(), eq(Set.of())))
                .thenReturn(List.of(new ResolvedPublisher(10L, "publisher", 4, parameters)));
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
                        true, List.of(new PublisherInput("publisher", 4, parameters)), Set.of());
    }

    @Test
    void explicitPublishersDoNotConsultTheWorkspaceOrigin() {
        setupScope();
        List<PublisherInput> explicit =
                List.of(new PublisherInput("publisher", 1, json.createObjectNode()));
        when(validator.validatePublishers(true, explicit, Set.of())).thenReturn(List.of());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        commands.create(
                "workspace",
                "application",
                new CreateApplicationEnvironmentConfigurationInput(
                        null, "environment", json.createObjectNode(), explicit, false));
        verifyNoInteractions(origin);
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
                .thenReturn(List.of(new PublisherCopy("publisher", 1, parameters)));
        when(associations.findByConfigurationIdOrderBySortOrderAsc(9L))
                .thenReturn(
                        List.of(
                                new ApplicationEnvironmentConfigurationPublisher(
                                        configuration, 10L, 1, parameters)));
        when(validator.validatePublishers(eq(true), anyList(), eq(Set.of())))
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
