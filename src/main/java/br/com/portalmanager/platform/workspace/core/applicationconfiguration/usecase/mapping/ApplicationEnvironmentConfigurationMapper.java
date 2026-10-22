package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.ApplicationEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.ApplicationEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.ApplicationEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.foundation.integration.EnvironmentReferencePort;

import org.springframework.stereotype.Component;

@Component
public class ApplicationEnvironmentConfigurationMapper {
    private final ApplicationEnvironmentConfigurationPublisherRepository associations;
    private final EnvironmentReferencePort environments;

    public ApplicationEnvironmentConfigurationMapper(
            ApplicationEnvironmentConfigurationPublisherRepository associations,
            EnvironmentReferencePort environments) {
        this.associations = associations;
        this.environments = environments;
    }

    public ApplicationEnvironmentConfigurationOutput toOutput(
            String workspaceIdentifier,
            String applicationIdentifier,
            ApplicationEnvironmentConfiguration configuration) {
        java.util.List<
                        br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain
                                .ApplicationEnvironmentConfigurationPublisher>
                stored =
                        associations.findByConfigurationIdOrderBySortOrderAsc(
                                configuration.getId());
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
                                                association.getPublisherCode(),
                                                association.getSortOrder(),
                                                association.getParameters()))
                        .toList(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt());
    }
}
