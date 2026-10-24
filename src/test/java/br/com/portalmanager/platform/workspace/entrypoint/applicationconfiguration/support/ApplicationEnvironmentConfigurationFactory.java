package br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.request.CreateApplicationEnvironmentConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.request.UpdateApplicationEnvironmentConfigurationRequest;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

public final class ApplicationEnvironmentConfigurationFactory extends AbstractTestDataFactory<CreateApplicationEnvironmentConfigurationRequest, ApplicationEnvironmentConfigurationBuilder> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Override protected ApplicationEnvironmentConfigurationBuilder builder() { return ApplicationEnvironmentConfigurationBuilder.builder(); }
    public CreateApplicationEnvironmentConfigurationRequest valid(String environmentIdentifier) { return builder().withEnvironment(environmentIdentifier).build(); }
    public CreateApplicationEnvironmentConfigurationRequest withoutEnvironment() { return builder().withEnvironment(null).build(); }
    public CreateApplicationEnvironmentConfigurationRequest withoutPublishers(String environmentIdentifier) { return builder().withEnvironment(environmentIdentifier).withPublishers(null).build(); }
    public CreateApplicationEnvironmentConfigurationRequest withInvalidSettings(String environmentIdentifier) { return builder().withEnvironment(environmentIdentifier).withSettings(JSON.createArrayNode()).build(); }
    public CreateApplicationEnvironmentConfigurationRequest withDuplicatePublisher(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withPublishers(List.of(
                new PublisherInput("KAFKA", 1, JSON.createObjectNode().put("topicName", "first-topic")),
                new PublisherInput("KAFKA", 2, JSON.createObjectNode().put("topicName", "second-topic")))).build();
    }
    public CreateApplicationEnvironmentConfigurationRequest withMissingPublisher(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withPublishers(List.of(new PublisherInput("UNKNOWN", 1, JSON.createObjectNode()))).build();
    }
    public CreateApplicationEnvironmentConfigurationRequest copyWorkspacePublishers(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).copyWorkspacePublishers().build();
    }
    public CreateApplicationEnvironmentConfigurationRequest withApplicationPublisher(String environmentIdentifier) {
        return builder().withEnvironment(environmentIdentifier).withPublishers(List.of(
                new PublisherInput("APP_CONFIG", 1, JSON.createObjectNode()
                        .put("arn", "arn:aws:appconfig:us-east-1:123456789012:application/test")
                        .put("contaAWS", "123456789012")))).build();
    }
    public UpdateApplicationEnvironmentConfigurationRequest update(Long version) { return ApplicationEnvironmentConfigurationUpdateBuilder.builder().withVersion(version).build(); }
    public UpdateApplicationEnvironmentConfigurationRequest updateCopyingWorkspacePublishers(Long version) {
        return ApplicationEnvironmentConfigurationUpdateBuilder.builder().withVersion(version).copyWorkspacePublishers().build();
    }
    public UpdateApplicationEnvironmentConfigurationRequest updateWithoutVersion() { return ApplicationEnvironmentConfigurationUpdateBuilder.builder().build(); }
    public UpdateApplicationEnvironmentConfigurationRequest updateWithInvalidSettings(Long version) {
        return ApplicationEnvironmentConfigurationUpdateBuilder.builder().withVersion(version).withSettings(JSON.createArrayNode()).build();
    }
    public UpdateApplicationEnvironmentConfigurationRequest updateWithoutPublishers(Long version) {
        return ApplicationEnvironmentConfigurationUpdateBuilder.builder().withVersion(version).withPublishers(null).build();
    }
}
