package br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.request.CreateApplicationEnvironmentConfigurationRequest;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class ApplicationEnvironmentConfigurationBuilder implements TestDataBuilder<CreateApplicationEnvironmentConfigurationRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Long version;
    private String environmentIdentifier;
    private JsonNode settings = JSON.createObjectNode().put("region", "test");
    private List<PublisherInput> publishers = List.of(new PublisherInput(
            "KAFKA", 1, JSON.createObjectNode().put("topicName", "test-topic")));
    private boolean copyWorkspacePublishers;

    public static ApplicationEnvironmentConfigurationBuilder builder() { return new ApplicationEnvironmentConfigurationBuilder(); }
    public ApplicationEnvironmentConfigurationBuilder withVersion(Long value) { version = value; return this; }
    public ApplicationEnvironmentConfigurationBuilder withEnvironment(String value) { environmentIdentifier = value; return this; }
    public ApplicationEnvironmentConfigurationBuilder withSettings(JsonNode value) { settings = value; return this; }
    public ApplicationEnvironmentConfigurationBuilder withPublishers(List<PublisherInput> value) { publishers = value; return this; }
    public ApplicationEnvironmentConfigurationBuilder copyWorkspacePublishers() { copyWorkspacePublishers = true; publishers = List.of(); return this; }

    @Override public CreateApplicationEnvironmentConfigurationRequest build() {
        return new CreateApplicationEnvironmentConfigurationRequest(version, environmentIdentifier, settings, publishers, copyWorkspacePublishers);
    }
}
