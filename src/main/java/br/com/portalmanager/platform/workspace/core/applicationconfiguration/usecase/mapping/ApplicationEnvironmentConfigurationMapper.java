package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.ApplicationEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.ApplicationEnvironmentConfigurationPublisher;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.ApplicationEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.ApplicationEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.EnvironmentReferencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApplicationEnvironmentConfigurationMapper {
    private final ApplicationEnvironmentConfigurationPublisherRepository associations;
    private final EnvironmentReferencePort environments;

    public ApplicationEnvironmentConfigurationMapper(ApplicationEnvironmentConfigurationPublisherRepository associations, EnvironmentReferencePort environments) {
        this.associations = associations;
        this.environments = environments;
    }

    public ApplicationEnvironmentConfigurationOutput toOutput(String workspaceIdentifier, String applicationIdentifier, ApplicationEnvironmentConfiguration configuration) {
        return toOutputs(workspaceIdentifier, applicationIdentifier, List.of(configuration)).get(configuration.getId());
    }

    public Map<Long, ApplicationEnvironmentConfigurationOutput> toOutputs(String workspaceIdentifier, String applicationIdentifier, List<ApplicationEnvironmentConfiguration> configurations) {
        if (configurations.isEmpty()) return Map.of();
        Map<Long, List<ApplicationEnvironmentConfigurationPublisher>> stored = associations
                .findByConfigurationIdInOrderBySortOrderAsc(configurations.stream().map(ApplicationEnvironmentConfiguration::getId).toList())
                .stream().collect(Collectors.groupingBy(association -> association.getConfiguration().getId()));
        Map<Long, String> environmentIdentifiers = environments.resolveIdentifiers(configurations.stream()
                .map(ApplicationEnvironmentConfiguration::getEnvironmentId).distinct().toList());
        return configurations.stream().collect(Collectors.toMap(ApplicationEnvironmentConfiguration::getId, configuration ->
                new ApplicationEnvironmentConfigurationOutput(configuration.getIdentifier(), workspaceIdentifier, applicationIdentifier,
                        environmentIdentifiers.get(configuration.getEnvironmentId()), configuration.getVersion(),
                        configuration.getLifecycle().value(), configuration.getSettings(),
                        stored.getOrDefault(configuration.getId(), List.of()).stream().map(association ->
                                new PublisherOutput(association.getPublisherCode().value(), association.getSortOrder(),
                                        association.getParameters())).toList(),
                        configuration.getCreatedAt(), configuration.getUpdatedAt())));
    }
}
