package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentTypeCompatibilityQueryService {

    private final EnvironmentTypeCompatibilityRepository repository;

    public EnvironmentTypeCompatibilityQueryService(EnvironmentTypeCompatibilityRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public EnvironmentCompatibilityOutput find(String identifier) {
        return repository.findByIdentifier(identifier)
            .map(EnvironmentCompatibilityOutput::from)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.COMPATIBILITY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<EnvironmentCompatibilityOutput> list(String lifecycle) {
        LifecycleTypeCode effectiveLifecycle = LifecycleFilter.resolve(lifecycle);
        return repository
            .findByLifecycleOrderByIdAsc(effectiveLifecycle)
            .stream()
            .map(EnvironmentCompatibilityOutput::from)
            .toList();
    }
}
