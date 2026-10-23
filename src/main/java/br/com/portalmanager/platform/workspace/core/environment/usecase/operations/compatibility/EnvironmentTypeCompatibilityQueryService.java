package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.compatibility.EnvironmentCompatibilityMapper;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.compatibility.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnvironmentTypeCompatibilityQueryService {

    private final EnvironmentTypeCompatibilityRepository repository;
    private final EnvironmentCompatibilityMapper mapper;

    public EnvironmentTypeCompatibilityQueryService(
            EnvironmentTypeCompatibilityRepository repository,
            EnvironmentCompatibilityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EnvironmentCompatibilityOutput find(String identifier) {
        return repository
                .findByIdentifier(identifier)
                .map(mapper::toOutput)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        EnvironmentMessageKeys.COMPATIBILITY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<EnvironmentCompatibilityOutput> list(String lifecycle) {
        LifecycleTypeCode effectiveLifecycle = LifecycleFilter.resolve(lifecycle);
        return repository.findByLifecycleOrderByIdAsc(effectiveLifecycle).stream()
                .map(mapper::toOutput)
                .toList();
    }
}
