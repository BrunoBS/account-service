package br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.request.CreateWorkspaceEnvironmentConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.request.UpdateWorkspaceEnvironmentConfigurationRequest;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

public final class WorkspaceEnvironmentConfigurationFactory extends AbstractTestDataFactory<
        CreateWorkspaceEnvironmentConfigurationRequest, WorkspaceEnvironmentConfigurationBuilder> {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    protected WorkspaceEnvironmentConfigurationBuilder builder() {
        return WorkspaceEnvironmentConfigurationBuilder.builder();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest valid(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).build();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest withoutEnvironment() {
        return builder().withEnvironment(null).build();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest withInvalidSettings(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withSettings(JSON.createArrayNode()).build();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest withNullSettings(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withNullSettings().build();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest validWithWebSocket(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withPublishers(List.of(
                new PublisherInput("WEB_SOCKET", 1, JSON.createObjectNode().put("clientId", "test-client"))))
                .build();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest withMultiplePublishers(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withPublishers(List.of(
                new PublisherInput("WEB_SOCKET", 2, JSON.createObjectNode().put("clientId", "test-client")),
                new PublisherInput("KAFKA", 1, JSON.createObjectNode().put("topicName", "test-topic"))))
                .build();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest withoutPublishers(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withPublishers(null).build();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest withMissingPublisherParameters(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withPublishers(List.of(
                new PublisherInput("KAFKA", 1, JSON.createObjectNode()))).build();
    }

    public CreateWorkspaceEnvironmentConfigurationRequest withDuplicatePublisher(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withPublishers(List.of(
                new PublisherInput("KAFKA", 1, JSON.createObjectNode().put("topicName", "first-topic")),
                new PublisherInput("KAFKA", 2, JSON.createObjectNode().put("topicName", "second-topic")))).build();
    }

    public UpdateWorkspaceEnvironmentConfigurationRequest update(Long version) {
        return WorkspaceEnvironmentConfigurationUpdateBuilder.builder().withVersion(version).build();
    }

    public UpdateWorkspaceEnvironmentConfigurationRequest updateWithoutVersion() {
        return WorkspaceEnvironmentConfigurationUpdateBuilder.builder().build();
    }

    public UpdateWorkspaceEnvironmentConfigurationRequest updateWithInvalidSettings(Long version) {
        return WorkspaceEnvironmentConfigurationUpdateBuilder.builder()
                .withVersion(version).withSettings(JSON.createArrayNode()).build();
    }

    public UpdateWorkspaceEnvironmentConfigurationRequest updateWithoutPublishers(Long version) {
        return WorkspaceEnvironmentConfigurationUpdateBuilder.builder()
                .withVersion(version).withPublishers(null).build();
    }
}
