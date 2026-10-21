package br.com.portalmanager.platform.workspace.core.configuration.application.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.configuration.application.domain.ApplicationEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.configuration.application.repository.ApplicationEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.configuration.application.usecase.model.ApplicationEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.core.configuration.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.foundation.integration.EnvironmentReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort.PublisherReference;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApplicationEnvironmentConfigurationMapper {
    private final ApplicationEnvironmentConfigurationPublisherRepository associations;
    private final EnvironmentReferencePort environments;
    private final PublisherReferencePort publishers;

    public ApplicationEnvironmentConfigurationMapper(
            ApplicationEnvironmentConfigurationPublisherRepository associations,
            EnvironmentReferencePort environments,
            PublisherReferencePort publishers) {
        this.associations = associations;
        this.environments = environments;
        this.publishers = publishers;
    }

    public ApplicationEnvironmentConfigurationOutput toOutput(
            String workspaceIdentifier,
            String applicationIdentifier,
            ApplicationEnvironmentConfiguration configuration) {
        java.util.List<
                        br.com.portalmanager.platform.workspace.core.configuration.application
                                .domain.ApplicationEnvironmentConfigurationPublisher>
                stored =
                        associations.findByConfigurationIdOrderBySortOrderAsc(
                                configuration.getId());
        Map<Long, PublisherReference> references =
                publishers
                        .findReferences(
                                stored.stream()
                                        .map(association -> association.getPublisherId())
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(PublisherReference::id, reference -> reference));
        return new ApplicationEnvironmentConfigurationOutput(
                configuration.getIdentifier(),
                workspaceIdentifier,
                applicationIdentifier,
                environments.resolveIdentifier(configuration.getEnvironmentId()),
                configuration.getVersion(),
                configuration.getLifecycle().value(),
                configuration.getSettings(),
                stored.stream()
                        .map(
                                association ->
                                        new PublisherOutput(
                                                references
                                                        .get(association.getPublisherId())
                                                        .identifier(),
                                                association.getSortOrder(),
                                                association.getParameters()))
                        .toList(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt());
    }
}
