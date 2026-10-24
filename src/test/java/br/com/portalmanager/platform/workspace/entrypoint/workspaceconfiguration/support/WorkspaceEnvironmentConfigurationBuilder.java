package br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.request.CreateWorkspaceEnvironmentConfigurationRequest;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class WorkspaceEnvironmentConfigurationBuilder
        implements TestDataBuilder<CreateWorkspaceEnvironmentConfigurationRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Long version;
    private String environmentIdentifier;
    private JsonNode settings = JSON.createObjectNode().put("region", "test");
    private List<PublisherInput> publishers = List.of(new PublisherInput(
            "KAFKA", 1, JSON.createObjectNode().put("topicName", "test-topic")));

    public static WorkspaceEnvironmentConfigurationBuilder builder() {
        return new WorkspaceEnvironmentConfigurationBuilder();
    }

    public WorkspaceEnvironmentConfigurationBuilder withVersion(Long value) {
        version = value;
        return this;
    }

    public WorkspaceEnvironmentConfigurationBuilder withEnvironment(String value) {
        environmentIdentifier = value;
        return this;
    }

    public WorkspaceEnvironmentConfigurationBuilder withSettings(JsonNode value) {
        settings = value;
        return this;
    }

    public WorkspaceEnvironmentConfigurationBuilder withNullSettings() {
        settings = JSON.nullNode();
        return this;
    }

    public WorkspaceEnvironmentConfigurationBuilder withPublishers(List<PublisherInput> value) {
        publishers = value;
        return this;
    }

    @Override
    public CreateWorkspaceEnvironmentConfigurationRequest build() {
        return new CreateWorkspaceEnvironmentConfigurationRequest(
                version, environmentIdentifier, settings, publishers);
    }
}
