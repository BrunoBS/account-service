package br.com.portalmanager.platform.workspace.core.configuration.workspace.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.configuration.validation.EnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.repository.WorkspaceEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.usecase.mapping.WorkspaceEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.usecase.model.WorkspaceEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceEnvironmentConfigurationQueryService {
    private final WorkspaceEnvironmentConfigurationRepository repository;
    private final WorkspaceEnvironmentConfigurationScope scopes;
    private final WorkspaceEnvironmentConfigurationMapper mapper;
    private final EnvironmentConfigurationValidator validator;

    public WorkspaceEnvironmentConfigurationQueryService(
            WorkspaceEnvironmentConfigurationRepository repository,
            WorkspaceEnvironmentConfigurationScope scopes,
            WorkspaceEnvironmentConfigurationMapper mapper,
            EnvironmentConfigurationValidator validator) {
        this.repository = repository;
        this.scopes = scopes;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public WorkspaceEnvironmentConfigurationOutput find(
            String workspaceIdentifier, String identifier) {
        Long ownerId = scopes.resolve(workspaceIdentifier, false).ownerId();
        return mapper.toOutput(
                workspaceIdentifier,
                repository
                        .findByIdentifierAndWorkspaceId(identifier, ownerId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PlatformMessageKeys.RESOURCE_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public Page<WorkspaceEnvironmentConfigurationOutput> findAll(
            String workspaceIdentifier, String lifecycle, Pageable pageable) {
        validator.validatePagination(pageable);
        Long ownerId = scopes.resolve(workspaceIdentifier, false).ownerId();
        Sort sort =
                pageable.getSort().getOrderFor("id") == null
                        ? pageable.getSort().and(Sort.by("id"))
                        : pageable.getSort();
        return repository
                .findByWorkspaceIdAndLifecycle(
                        ownerId,
                        LifecycleFilter.resolve(lifecycle),
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort))
                .map(configuration -> mapper.toOutput(workspaceIdentifier, configuration));
    }
}
