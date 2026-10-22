package br.com.portalmanager.platform.workspace.core.workspaceconfiguration;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfigurationPublisher;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.UUID;

class EnvironmentConfigurationDomainTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void independentInstancesCopyJsonAndHavePublicUuidAndRestrictedLifecycle() {
        LocalDateTime now = LocalDateTime.now();
        ObjectNode settings = mapper.createObjectNode().put("limit", 10);
        WorkspaceEnvironmentConfiguration configuration =
                new WorkspaceEnvironmentConfiguration(1L, 2L, settings, now);
        WorkspaceEnvironmentConfiguration another =
                new WorkspaceEnvironmentConfiguration(3L, 2L, settings, now);
        assertThat(UUID.fromString(configuration.getIdentifier())).isNotNull();
        assertThat(configuration.getIdentifier()).isNotEqualTo(another.getIdentifier());
        settings.put("limit", 20);
        ((ObjectNode) configuration.getSettings()).put("limit", 30);
        assertThat(configuration.getSettings().get("limit").asInt()).isEqualTo(10);
        assertThat(another.getSettings().get("limit").asInt()).isEqualTo(10);
        assertThatThrownBy(() -> configuration.quarantine(now))
                .isInstanceOf(ValidationException.class);
        configuration.inactivate(now);
        assertThat(configuration.getLifecycle().value()).isEqualTo("INACTIVE");
        assertThatThrownBy(() -> configuration.updateSettings(settings, now))
                .isInstanceOf(ValidationException.class);
        configuration.quarantine(now);
        assertThat(configuration.getLifecycle().value()).isEqualTo("QUARANTINED");
        assertThatThrownBy(() -> configuration.inactivate(now))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void associationsOwnIndependentParametersAndRequirePositiveOrder() {
        WorkspaceEnvironmentConfiguration configuration =
                new WorkspaceEnvironmentConfiguration(
                        1L, 2L, mapper.createObjectNode(), LocalDateTime.now());
        ObjectNode parameters = mapper.createObjectNode().put("destination", "one");
        WorkspaceEnvironmentConfigurationPublisher publisher =
                new WorkspaceEnvironmentConfigurationPublisher(configuration, 3L, 1, parameters);
        parameters.put("destination", "two");
        ((ObjectNode) publisher.getParameters()).put("destination", "three");
        assertThat(publisher.getParameters().get("destination").asString()).isEqualTo("one");
        assertThatThrownBy(
                        () ->
                                new WorkspaceEnvironmentConfigurationPublisher(
                                        configuration, 3L, 0, parameters))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
