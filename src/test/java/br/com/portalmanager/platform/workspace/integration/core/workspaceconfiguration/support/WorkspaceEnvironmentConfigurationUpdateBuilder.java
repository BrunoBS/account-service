package br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.request.UpdateWorkspaceEnvironmentConfigurationRequest;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class WorkspaceEnvironmentConfigurationUpdateBuilder
        implements TestDataBuilder<UpdateWorkspaceEnvironmentConfigurationRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Long version;
    private JsonNode settings = JSON.createObjectNode().put("region", "updated");
    private List<PublisherInput> publishers = List.of(new PublisherInput(
            "KAFKA", 1, JSON.createObjectNode().put("topicName", "updated-topic")));

    public static WorkspaceEnvironmentConfigurationUpdateBuilder builder() {
        return new WorkspaceEnvironmentConfigurationUpdateBuilder();
    }

    public WorkspaceEnvironmentConfigurationUpdateBuilder withVersion(Long value) {
        version = value;
        return this;
    }

    public WorkspaceEnvironmentConfigurationUpdateBuilder withSettings(JsonNode value) {
        settings = value;
        return this;
    }

    public WorkspaceEnvironmentConfigurationUpdateBuilder withPublishers(List<PublisherInput> value) {
        publishers = value;
        return this;
    }

    @Override
    public UpdateWorkspaceEnvironmentConfigurationRequest build() {
        return new UpdateWorkspaceEnvironmentConfigurationRequest(version, settings, publishers);
    }
}
