package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.ApplicationEnvironmentConfiguration;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.ApplicationEnvironmentConfigurationPublisher;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.ApplicationEnvironmentConfigurationPublisherRepository;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.ApplicationEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.mapping.ApplicationEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.ResolvedPublisher;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.validation.ApplicationEnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.EnvironmentReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceEnvironmentConfigurationPort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationEnvironmentConfigurationCommandService {
    private final ApplicationEnvironmentConfigurationRepository repository;
    private final ApplicationEnvironmentConfigurationPublisherRepository associations;
    private final ApplicationEnvironmentConfigurationScope scopes;
    private final EnvironmentReferencePort environments;
    private final ApplicationEnvironmentConfigurationValidator validator;
    private final ApplicationEnvironmentConfigurationMapper mapper;
    private final WorkspaceEnvironmentConfigurationPort workspaceConfigurations;

    public ApplicationEnvironmentConfigurationCommandService(
            ApplicationEnvironmentConfigurationRepository repository,
            ApplicationEnvironmentConfigurationPublisherRepository associations,
            ApplicationEnvironmentConfigurationScope scopes,
            EnvironmentReferencePort environments,
            ApplicationEnvironmentConfigurationValidator validator,
            ApplicationEnvironmentConfigurationMapper mapper,
            WorkspaceEnvironmentConfigurationPort workspaceConfigurations) {
        this.repository = repository;
        this.associations = associations;
        this.scopes = scopes;
        this.environments = environments;
        this.validator = validator;
        this.mapper = mapper;
        this.workspaceConfigurations = workspaceConfigurations;
    }

    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "APPLICATION_ENVIRONMENT_CONFIGURATION_CREATED",
            resourceType = "APPLICATION_ENVIRONMENT_CONFIGURATION")
    public ApplicationEnvironmentConfigurationOutput create(
            String workspaceIdentifier,
            String applicationIdentifier,
            CreateApplicationEnvironmentConfigurationInput input) {
        validator.validateCreateVersion(input.version());
        ApplicationEnvironmentConfigurationScope.Scope scope =
                scopes.resolve(workspaceIdentifier, applicationIdentifier, true);
        Long environmentId =
                environments.resolveActiveForUpdate(
                        scope.workspaceId(), input.environmentIdentifier());
        validator.validateSettings(input.settings());
        validator.validateCopy(input.copyWorkspacePublishers(), input.publishers());
        List<PublisherInput> publisherInput =
                input.copyWorkspacePublishers()
                        ? workspaceConfigurations
                                .copyPublishers(scope.workspaceId(), environmentId)
                                .stream()
                                .map(
                                        publisher ->
                                                new PublisherInput(
                                                        publisher.publisherCode(),
                                                        publisher.sortOrder(),
                                                        publisher.parameters()))
                                .toList()
                        : input.publishers();
        List<ResolvedPublisher> selected = validator.validatePublishers(publisherInput, Set.of());
        if (input.copyWorkspacePublishers())
            validator.validateCopiedRequest(
                    input.environmentIdentifier(), null, input.settings(), publisherInput);
        ApplicationEnvironmentConfiguration configuration =
                repository.saveAndFlush(
                        new ApplicationEnvironmentConfiguration(
                                scope.ownerId(),
                                environmentId,
                                input.settings(),
                                LocalDateTime.now()));
        persistAssociations(configuration, selected);
        return mapper.toOutput(workspaceIdentifier, applicationIdentifier, configuration);
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "APPLICATION_ENVIRONMENT_CONFIGURATION_UPDATED",
            resourceType = "APPLICATION_ENVIRONMENT_CONFIGURATION")
    public ApplicationEnvironmentConfigurationOutput update(
            String workspaceIdentifier,
            String applicationIdentifier,
            String identifier,
            UpdateApplicationEnvironmentConfigurationInput input) {
        ApplicationEnvironmentConfigurationScope.Scope scope =
                scopes.resolve(workspaceIdentifier, applicationIdentifier, true);
        ApplicationEnvironmentConfiguration configuration = find(scope.ownerId(), identifier);
        validator.validateUpdateVersion(input.version(), configuration.getVersion());
        Long environmentId = configuration.getEnvironmentId();
        environments.resolveActiveForUpdate(
                scope.workspaceId(), environments.resolveIdentifier(environmentId));
        validator.validateSettings(input.settings());
        validator.validateCopy(input.copyWorkspacePublishers(), input.publishers());
        List<PublisherInput> publisherInput =
                input.copyWorkspacePublishers()
                        ? workspaceConfigurations
                                .copyPublishers(scope.workspaceId(), environmentId)
                                .stream()
                                .map(
                                        publisher ->
                                                new PublisherInput(
                                                        publisher.publisherCode(),
                                                        publisher.sortOrder(),
                                                        publisher.parameters()))
                                .toList()
                        : input.publishers();
        Set<String> existingIds =
                associations
                        .findByConfigurationIdOrderBySortOrderAsc(configuration.getId())
                        .stream()
                        .map(ApplicationEnvironmentConfigurationPublisher::getPublisherCode)
                        .collect(Collectors.toSet());
        List<ResolvedPublisher> selected =
                validator.validatePublishers(
                        publisherInput, input.copyWorkspacePublishers() ? Set.of() : existingIds);
        if (input.copyWorkspacePublishers())
            validator.validateCopiedRequest(
                    null, input.version(), input.settings(), publisherInput);
        configuration.updateSettings(input.settings(), LocalDateTime.now());
        repository.saveAndFlush(configuration);
        associations.deleteAssociations(configuration.getId());
        persistAssociations(configuration, selected);
        return mapper.toOutput(workspaceIdentifier, applicationIdentifier, configuration);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "APPLICATION_ENVIRONMENT_CONFIGURATION_DEACTIVATED",
            resourceType = "APPLICATION_ENVIRONMENT_CONFIGURATION")
    public ApplicationEnvironmentConfigurationOutput inactivate(
            String workspaceIdentifier, String applicationIdentifier, String identifier) {
        ApplicationEnvironmentConfigurationScope.Scope scope =
                scopes.resolve(workspaceIdentifier, applicationIdentifier, true);
        ApplicationEnvironmentConfiguration configuration = find(scope.ownerId(), identifier);
        configuration.inactivate(LocalDateTime.now());
        repository.saveAndFlush(configuration);
        return mapper.toOutput(workspaceIdentifier, applicationIdentifier, configuration);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DELETE,
            event = "APPLICATION_ENVIRONMENT_CONFIGURATION_QUARANTINED",
            resourceType = "APPLICATION_ENVIRONMENT_CONFIGURATION")
    public ApplicationEnvironmentConfigurationOutput quarantine(
            String workspaceIdentifier, String applicationIdentifier, String identifier) {
        ApplicationEnvironmentConfigurationScope.Scope scope =
                scopes.resolve(workspaceIdentifier, applicationIdentifier, true);
        ApplicationEnvironmentConfiguration configuration = find(scope.ownerId(), identifier);
        configuration.quarantine(LocalDateTime.now());
        repository.saveAndFlush(configuration);
        return mapper.toOutput(workspaceIdentifier, applicationIdentifier, configuration);
    }

    private ApplicationEnvironmentConfiguration find(Long ownerId, String identifier) {
        return repository
                .findByIdentifierAndApplicationId(identifier, ownerId)
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.RESOURCE_NOT_FOUND));
    }

    private void persistAssociations(
            ApplicationEnvironmentConfiguration configuration, List<ResolvedPublisher> selected) {
        associations.saveAllAndFlush(
                selected.stream()
                        .map(
                                publisher ->
                                        new ApplicationEnvironmentConfigurationPublisher(
                                                configuration,
                                                publisher.publisherCode(),
                                                publisher.sortOrder(),
                                                publisher.parameters()))
                        .toList());
    }
}
