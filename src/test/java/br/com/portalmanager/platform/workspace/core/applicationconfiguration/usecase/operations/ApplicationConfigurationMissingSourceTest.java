package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.ApplicationEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.ApplicationEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.ApplicationEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.mapping.ApplicationEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.validation.ApplicationEnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.UpdateApplicationEnvironmentConfigurationInput;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/** Fault case: a persisted application configuration whose source has disappeared.
 * HTTP never physically deletes a workspace configuration, so this cannot be arranged through the public lifecycle.
 */
class ApplicationConfigurationMissingSourceTest {
    @Test void missingSourceBlocksExplicitUpdateBeforeMutation() { assertMissingSource(false); }
    @Test void missingSourceBlocksCopiedUpdateBeforeMutation() { assertMissingSource(true); }

    private void assertMissingSource(boolean copy) {
        ApplicationEnvironmentConfigurationRepository repository = mock(ApplicationEnvironmentConfigurationRepository.class);
        ApplicationEnvironmentConfigurationPublisherRepository associations = mock(ApplicationEnvironmentConfigurationPublisherRepository.class);
        ApplicationEnvironmentConfigurationScope scopes = mock(ApplicationEnvironmentConfigurationScope.class);
        EnvironmentReferencePort environments = mock(EnvironmentReferencePort.class);
        WorkspaceEnvironmentConfigurationPort source = mock(WorkspaceEnvironmentConfigurationPort.class);
        ApplicationEnvironmentConfigurationValidator validator = mock(ApplicationEnvironmentConfigurationValidator.class);
        PhaseCompletionPort phases = mock(PhaseCompletionPort.class);
        JsonNode settings = JsonMapper.builder().build().createObjectNode().put("region", "original");
        ApplicationEnvironmentConfiguration configuration = new ApplicationEnvironmentConfiguration(2L, 3L, settings, LocalDateTime.now());
        when(scopes.resolve("workspace", "application", true)).thenReturn(new ApplicationEnvironmentConfigurationScope.Scope(1L, 2L));
        when(repository.findByIdentifierAndApplicationId(configuration.getIdentifier(), 2L)).thenReturn(Optional.of(configuration));
        when(environments.resolveIdentifier(3L)).thenReturn("environment");
        NotFoundException failure = new NotFoundException(PlatformMessageKeys.RESOURCE_NOT_FOUND);
        if (copy) when(source.copyPublishers(1L, 3L)).thenThrow(failure);
        else doThrow(failure).when(source).requireActive(1L, 3L);
        ApplicationEnvironmentConfigurationCommandService command = new ApplicationEnvironmentConfigurationCommandService(repository, associations, scopes, environments, validator, mock(ApplicationEnvironmentConfigurationMapper.class), source, phases);
        assertThatThrownBy(() -> command.update("workspace", "application", configuration.getIdentifier(), new UpdateApplicationEnvironmentConfigurationInput(0L, JsonMapper.builder().build().createObjectNode().put("region", "changed"), List.of(), copy))).isSameAs(failure);
        assertThat(configuration.getSettings()).isEqualTo(settings);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(associations, phases);
        if (!copy) verify(source, never()).copyPublishers(any(), any());
    }
}
