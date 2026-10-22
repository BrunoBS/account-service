package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.WorkspaceEnvironmentConfigurationPublisher;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.WorkspaceEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.mapping.WorkspaceEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.ResolvedPublisher;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.validation.WorkspaceEnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.EnvironmentReferencePort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkspaceEnvironmentConfigurationCommandService {
    private final WorkspaceEnvironmentConfigurationRepository repository;
    private final WorkspaceEnvironmentConfigurationPublisherRepository associations;
    private final WorkspaceEnvironmentConfigurationScope scopes;
    private final EnvironmentReferencePort environments;
    private final WorkspaceEnvironmentConfigurationValidator validator;
    private final WorkspaceEnvironmentConfigurationMapper mapper;

    public WorkspaceEnvironmentConfigurationCommandService(
            WorkspaceEnvironmentConfigurationRepository repository,
            WorkspaceEnvironmentConfigurationPublisherRepository associations,
            WorkspaceEnvironmentConfigurationScope scopes,
            EnvironmentReferencePort environments,
            WorkspaceEnvironmentConfigurationValidator validator,
            WorkspaceEnvironmentConfigurationMapper mapper) {
        this.repository = repository;
        this.associations = associations;
        this.scopes = scopes;
        this.environments = environments;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "WORKSPACE_ENVIRONMENT_CONFIGURATION_CREATED",
            resourceType = "WORKSPACE_ENVIRONMENT_CONFIGURATION")
    public WorkspaceEnvironmentConfigurationOutput create(
            String workspaceIdentifier, CreateWorkspaceEnvironmentConfigurationInput input) {
        validator.validateCreateVersion(input.version());
        WorkspaceEnvironmentConfigurationScope.Scope scope =
                scopes.resolve(workspaceIdentifier, true);
        Long environmentId =
                environments.resolveActiveForUpdate(
                        scope.workspaceId(), input.environmentIdentifier());
        validator.validateSettings(input.settings());
        List<PublisherInput> publisherInput = input.publishers();
        List<ResolvedPublisher> selected = validator.validatePublishers(publisherInput, Set.of());
        WorkspaceEnvironmentConfiguration configuration =
                repository.saveAndFlush(
                        new WorkspaceEnvironmentConfiguration(
                                scope.ownerId(),
                                environmentId,
                                input.settings(),
                                LocalDateTime.now()));
        persistAssociations(configuration, selected);
        return mapper.toOutput(workspaceIdentifier, configuration);
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "WORKSPACE_ENVIRONMENT_CONFIGURATION_UPDATED",
            resourceType = "WORKSPACE_ENVIRONMENT_CONFIGURATION")
    public WorkspaceEnvironmentConfigurationOutput update(
            String workspaceIdentifier,
            String identifier,
            UpdateWorkspaceEnvironmentConfigurationInput input) {
        WorkspaceEnvironmentConfigurationScope.Scope scope =
                scopes.resolve(workspaceIdentifier, true);
        WorkspaceEnvironmentConfiguration configuration = findConfigurationOrThrow(scope.ownerId(), identifier);
        validator.validateUpdateVersion(input.version(), configuration.getVersion());
        Long environmentId = configuration.getEnvironmentId();
        environments.resolveActiveForUpdate(
                scope.workspaceId(), environments.resolveIdentifier(environmentId));
        validator.validateSettings(input.settings());
        List<PublisherInput> publisherInput = input.publishers();
        Set<String> existingIds =
                associations
                        .findByConfigurationIdOrderBySortOrderAsc(configuration.getId())
                        .stream()
                        .map(WorkspaceEnvironmentConfigurationPublisher::getPublisherCode)
                        .map(PublisherTypeCode::value)
                        .collect(Collectors.toSet());
        List<ResolvedPublisher> selected =
                validator.validatePublishers(publisherInput, existingIds);
        configuration.updateSettings(input.settings(), LocalDateTime.now());
        repository.saveAndFlush(configuration);
        associations.deleteAssociations(configuration.getId());
        persistAssociations(configuration, selected);
        return mapper.toOutput(workspaceIdentifier, configuration);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "WORKSPACE_ENVIRONMENT_CONFIGURATION_DEACTIVATED",
            resourceType = "WORKSPACE_ENVIRONMENT_CONFIGURATION")
    public WorkspaceEnvironmentConfigurationOutput inactivate(
            String workspaceIdentifier, String identifier) {
        WorkspaceEnvironmentConfigurationScope.Scope scope =
                scopes.resolve(workspaceIdentifier, true);
        WorkspaceEnvironmentConfiguration configuration = findConfigurationOrThrow(scope.ownerId(), identifier);
        configuration.inactivate(LocalDateTime.now());
        repository.saveAndFlush(configuration);
        return mapper.toOutput(workspaceIdentifier, configuration);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DELETE,
            event = "WORKSPACE_ENVIRONMENT_CONFIGURATION_QUARANTINED",
            resourceType = "WORKSPACE_ENVIRONMENT_CONFIGURATION")
    public WorkspaceEnvironmentConfigurationOutput quarantine(
            String workspaceIdentifier, String identifier) {
        WorkspaceEnvironmentConfigurationScope.Scope scope =
                scopes.resolve(workspaceIdentifier, true);
        WorkspaceEnvironmentConfiguration configuration = findConfigurationOrThrow(scope.ownerId(), identifier);
        configuration.quarantine(LocalDateTime.now());
        repository.saveAndFlush(configuration);
        return mapper.toOutput(workspaceIdentifier, configuration);
    }

    private WorkspaceEnvironmentConfiguration findConfigurationOrThrow(Long ownerId, String identifier) {
        return repository
                .findByIdentifierAndWorkspaceId(identifier, ownerId)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.RESOURCE_NOT_FOUND));
    }

    private void persistAssociations(
            WorkspaceEnvironmentConfiguration configuration, List<ResolvedPublisher> selected) {
        associations.saveAllAndFlush(
                selected.stream()
                        .map(
                                publisher ->
                                        new WorkspaceEnvironmentConfigurationPublisher(
                                                configuration,
                                                PublisherTypeCode.of(publisher.publisherCode()),
                                                publisher.sortOrder(),
                                                publisher.parameters()))
                        .toList());
    }
}
