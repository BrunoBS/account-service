package br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.repository.ApplicationEnvironmentConfigurationRepository;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.mapping.ApplicationEnvironmentConfigurationMapper;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.model.ApplicationEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.validation.ApplicationEnvironmentConfigurationValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationEnvironmentConfigurationQueryService {
    private final ApplicationEnvironmentConfigurationRepository repository;
    private final ApplicationEnvironmentConfigurationScope scopes;
    private final ApplicationEnvironmentConfigurationMapper mapper;
    private final ApplicationEnvironmentConfigurationValidator validator;

    public ApplicationEnvironmentConfigurationQueryService(
            ApplicationEnvironmentConfigurationRepository repository,
            ApplicationEnvironmentConfigurationScope scopes,
            ApplicationEnvironmentConfigurationMapper mapper,
            ApplicationEnvironmentConfigurationValidator validator) {
        this.repository = repository;
        this.scopes = scopes;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public ApplicationEnvironmentConfigurationOutput find(
            String workspaceIdentifier, String applicationIdentifier, String identifier) {
        Long ownerId = scopes.resolve(workspaceIdentifier, applicationIdentifier, false).ownerId();
        return mapper.toOutput(
                workspaceIdentifier,
                applicationIdentifier,
                repository
                        .findByIdentifierAndApplicationId(identifier, ownerId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PlatformMessageKeys.RESOURCE_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationEnvironmentConfigurationOutput> findAll(
            String workspaceIdentifier,
            String applicationIdentifier,
            String lifecycle,
            Pageable pageable) {
        validator.validatePagination(pageable);
        Long ownerId = scopes.resolve(workspaceIdentifier, applicationIdentifier, false).ownerId();
        Sort sort =
                pageable.getSort().getOrderFor("id") == null
                        ? pageable.getSort().and(Sort.by("id"))
                        : pageable.getSort();
        return repository
                .findByApplicationIdAndLifecycle(
                        ownerId,
                        LifecycleFilter.resolve(lifecycle),
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort))
                .map(
                        configuration ->
                                mapper.toOutput(
                                        workspaceIdentifier, applicationIdentifier, configuration));
    }
}
