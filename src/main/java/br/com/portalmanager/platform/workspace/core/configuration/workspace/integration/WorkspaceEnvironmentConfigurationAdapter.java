package br.com.portalmanager.platform.workspace.core.configuration.workspace.integration;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.repository.WorkspaceEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.repository.WorkspaceEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort.PublisherReference;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceEnvironmentConfigurationPort;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceEnvironmentConfigurationPort.PublisherCopy;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorkspaceEnvironmentConfigurationAdapter
        implements WorkspaceEnvironmentConfigurationPort {
    private final WorkspaceEnvironmentConfigurationRepository repository;
    private final WorkspaceEnvironmentConfigurationPublisherRepository associations;
    private final PublisherReferencePort publishers;

    public WorkspaceEnvironmentConfigurationAdapter(
            WorkspaceEnvironmentConfigurationRepository repository,
            WorkspaceEnvironmentConfigurationPublisherRepository associations,
            PublisherReferencePort publishers) {
        this.repository = repository;
        this.associations = associations;
        this.publishers = publishers;
    }

    @Override
    @Transactional
    public List<PublisherCopy> copyPublishers(Long workspaceId, Long environmentId) {
        WorkspaceEnvironmentConfiguration configuration =
                repository
                        .findByWorkspaceIdAndEnvironmentId(workspaceId, environmentId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PlatformMessageKeys.RESOURCE_NOT_FOUND));
        List<
                        br.com.portalmanager.platform.workspace.core.configuration.workspace.domain
                                .WorkspaceEnvironmentConfigurationPublisher>
                source =
                        associations.findByConfigurationIdOrderBySortOrderAsc(
                                configuration.getId());
        Map<Long, PublisherReference> references =
                publishers
                        .findReferences(
                                source.stream()
                                        .map(association -> association.getPublisherId())
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(PublisherReference::id, reference -> reference));
        return source.stream()
                .map(
                        association ->
                                new PublisherCopy(
                                        references.get(association.getPublisherId()).identifier(),
                                        association.getSortOrder(),
                                        association.getParameters()))
                .toList();
    }
}
