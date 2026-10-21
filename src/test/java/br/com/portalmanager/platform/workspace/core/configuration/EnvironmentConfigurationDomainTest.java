package br.com.portalmanager.platform.workspace.core.configuration;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.configuration.application.domain.ApplicationEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.domain.WorkspaceEnvironmentConfigurationPublisher;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.UUID;

class EnvironmentConfigurationDomainTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void independentAggregatesCopyJsonAndHavePublicUuidAndRestrictedLifecycle() {
        LocalDateTime now = LocalDateTime.now();
        ObjectNode settings = mapper.createObjectNode().put("limit", 10);
        WorkspaceEnvironmentConfiguration workspace =
                new WorkspaceEnvironmentConfiguration(1L, 2L, settings, now);
        ApplicationEnvironmentConfiguration application =
                new ApplicationEnvironmentConfiguration(3L, 2L, settings, now);
        assertThat(UUID.fromString(workspace.getIdentifier())).isNotNull();
        assertThat(workspace.getIdentifier()).isNotEqualTo(application.getIdentifier());
        settings.put("limit", 20);
        ((ObjectNode) workspace.getSettings()).put("limit", 30);
        assertThat(workspace.getSettings().get("limit").asInt()).isEqualTo(10);
        assertThat(application.getSettings().get("limit").asInt()).isEqualTo(10);
        assertThatThrownBy(() -> workspace.quarantine(now)).isInstanceOf(ValidationException.class);
        workspace.inactivate(now);
        assertThat(workspace.getLifecycle().value()).isEqualTo("INACTIVE");
        assertThatThrownBy(() -> workspace.updateSettings(settings, now))
                .isInstanceOf(ValidationException.class);
        workspace.quarantine(now);
        assertThat(workspace.getLifecycle().value()).isEqualTo("QUARANTINED");
        assertThatThrownBy(() -> workspace.inactivate(now)).isInstanceOf(ValidationException.class);
        application.inactivate(now);
        application.quarantine(now);
        assertThat(application.getLifecycle().value()).isEqualTo("QUARANTINED");
    }

    @Test
    void associationsOwnIndependentParametersAndRequirePositiveOrder() {
        LocalDateTime now = LocalDateTime.now();
        ObjectNode settings = mapper.createObjectNode();
        WorkspaceEnvironmentConfiguration configuration =
                new WorkspaceEnvironmentConfiguration(1L, 2L, settings, now);
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
