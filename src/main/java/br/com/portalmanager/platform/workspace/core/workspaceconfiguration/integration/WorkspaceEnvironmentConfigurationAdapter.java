package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.integration;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.WorkspaceEnvironmentConfigurationPort;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.WorkspaceEnvironmentConfigurationPort.PublisherCopy;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfigurationPublisher;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class WorkspaceEnvironmentConfigurationAdapter
        implements WorkspaceEnvironmentConfigurationPort {
    private final WorkspaceEnvironmentConfigurationRepository repository;
    private final WorkspaceEnvironmentConfigurationPublisherRepository associations;

    public WorkspaceEnvironmentConfigurationAdapter(
            WorkspaceEnvironmentConfigurationRepository repository,
            WorkspaceEnvironmentConfigurationPublisherRepository associations) {
        this.repository = repository;
        this.associations = associations;
    }

    @Override
    @Transactional
    public void requireActive(Long workspaceId, Long environmentId) {
        findActive(workspaceId, environmentId);
    }

    @Override
    @Transactional
    public List<PublisherCopy> copyPublishers(Long workspaceId, Long environmentId) {
        WorkspaceEnvironmentConfiguration configuration = findActive(workspaceId, environmentId);
        List<WorkspaceEnvironmentConfigurationPublisher> source =
                        associations.findByConfigurationIdOrderBySortOrderAsc(
                                configuration.getId());
        return source.stream()
                .map(
                        association ->
                                new PublisherCopy(
                                        association.getPublisherCode().value(),
                                        association.getSortOrder(),
                                        association.getParameters()))
                .toList();
    }

    private WorkspaceEnvironmentConfiguration findActive(Long workspaceId, Long environmentId) {
        return repository.findByWorkspaceIdAndEnvironmentIdAndLifecycle(
                        workspaceId, environmentId, LifecycleTypeCode.active())
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.RESOURCE_NOT_FOUND));
    }
}
