package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.WorkspaceEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.foundation.integration.EnvironmentReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort.PublisherReference;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkspaceEnvironmentConfigurationMapper {
    private final WorkspaceEnvironmentConfigurationPublisherRepository associations;
    private final EnvironmentReferencePort environments;
    private final PublisherReferencePort publishers;

    public WorkspaceEnvironmentConfigurationMapper(
            WorkspaceEnvironmentConfigurationPublisherRepository associations,
            EnvironmentReferencePort environments,
            PublisherReferencePort publishers) {
        this.associations = associations;
        this.environments = environments;
        this.publishers = publishers;
    }

    public WorkspaceEnvironmentConfigurationOutput toOutput(
            String workspaceIdentifier, WorkspaceEnvironmentConfiguration configuration) {
        java.util.List<
                        br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain
                                .WorkspaceEnvironmentConfigurationPublisher>
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
        return new WorkspaceEnvironmentConfigurationOutput(
                configuration.getIdentifier(),
                workspaceIdentifier,
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
