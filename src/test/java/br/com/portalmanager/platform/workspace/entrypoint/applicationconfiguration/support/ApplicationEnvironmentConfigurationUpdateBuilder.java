package br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.request.UpdateApplicationEnvironmentConfigurationRequest;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class ApplicationEnvironmentConfigurationUpdateBuilder implements TestDataBuilder<UpdateApplicationEnvironmentConfigurationRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Long version;
    private JsonNode settings = JSON.createObjectNode().put("region", "updated");
    private List<PublisherInput> publishers = List.of(new PublisherInput(
            "KAFKA", 1, JSON.createObjectNode().put("topicName", "test-topic")));
    private boolean copyWorkspacePublishers;

    public static ApplicationEnvironmentConfigurationUpdateBuilder builder() { return new ApplicationEnvironmentConfigurationUpdateBuilder(); }
    public ApplicationEnvironmentConfigurationUpdateBuilder withVersion(Long value) { version = value; return this; }
    public ApplicationEnvironmentConfigurationUpdateBuilder withSettings(JsonNode value) { settings = value; return this; }
    public ApplicationEnvironmentConfigurationUpdateBuilder withPublishers(List<PublisherInput> value) { publishers = value; return this; }
    public ApplicationEnvironmentConfigurationUpdateBuilder copyWorkspacePublishers() { copyWorkspacePublishers = true; publishers = List.of(); return this; }

    @Override public UpdateApplicationEnvironmentConfigurationRequest build() {
        return new UpdateApplicationEnvironmentConfigurationRequest(version, settings, publishers, copyWorkspacePublishers);
    }
}
