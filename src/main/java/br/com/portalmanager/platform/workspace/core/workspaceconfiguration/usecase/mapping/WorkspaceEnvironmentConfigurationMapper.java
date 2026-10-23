package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.WorkspaceEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.EnvironmentReferencePort;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkspaceEnvironmentConfigurationMapper {
    private final WorkspaceEnvironmentConfigurationPublisherRepository associations;
    private final EnvironmentReferencePort environments;

    public WorkspaceEnvironmentConfigurationMapper(
            WorkspaceEnvironmentConfigurationPublisherRepository associations,
            EnvironmentReferencePort environments) {
        this.associations = associations;
        this.environments = environments;
    }

    public WorkspaceEnvironmentConfigurationOutput toOutput(
            String workspaceIdentifier, WorkspaceEnvironmentConfiguration configuration) {
        List<
                        br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain
                                .WorkspaceEnvironmentConfigurationPublisher>
                stored =
                        associations.findByConfigurationIdOrderBySortOrderAsc(
                                configuration.getId());
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
                                                association.getPublisherCode().value(),
                                                association.getSortOrder(),
                                                association.getParameters()))
                        .toList(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt());
    }
}
