package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfigurationPublisher;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.WorkspaceEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.EnvironmentReferencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkspaceEnvironmentConfigurationMapper {
    private final WorkspaceEnvironmentConfigurationPublisherRepository associations;
    private final EnvironmentReferencePort environments;

    public WorkspaceEnvironmentConfigurationMapper(WorkspaceEnvironmentConfigurationPublisherRepository associations, EnvironmentReferencePort environments) {
        this.associations = associations;
        this.environments = environments;
    }

    public WorkspaceEnvironmentConfigurationOutput toOutput(String workspaceIdentifier, WorkspaceEnvironmentConfiguration configuration) {
        return toOutputs(workspaceIdentifier, List.of(configuration)).get(configuration.getId());
    }

    public Map<Long, WorkspaceEnvironmentConfigurationOutput> toOutputs(String workspaceIdentifier, List<WorkspaceEnvironmentConfiguration> configurations) {
        if (configurations.isEmpty()) return Map.of();
        Map<Long, List<WorkspaceEnvironmentConfigurationPublisher>> stored = associations
                .findByConfigurationIdInOrderBySortOrderAsc(configurations.stream().map(WorkspaceEnvironmentConfiguration::getId).toList())
                .stream().collect(Collectors.groupingBy(association -> association.getConfiguration().getId()));
        Map<Long, String> environmentIdentifiers = environments.resolveIdentifiers(configurations.stream()
                .map(WorkspaceEnvironmentConfiguration::getEnvironmentId).distinct().toList());
        return configurations.stream().collect(Collectors.toMap(WorkspaceEnvironmentConfiguration::getId, configuration ->
                new WorkspaceEnvironmentConfigurationOutput(configuration.getIdentifier(), workspaceIdentifier,
                        environmentIdentifiers.get(configuration.getEnvironmentId()), configuration.getVersion(),
                        configuration.getLifecycle().value(), configuration.getSettings(),
                        stored.getOrDefault(configuration.getId(), List.of()).stream().map(association ->
                                new PublisherOutput(association.getPublisherCode().value(), association.getSortOrder(),
                                        association.getParameters())).toList(),
                        configuration.getCreatedAt(), configuration.getUpdatedAt())));
    }
}
