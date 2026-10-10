package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility;

import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
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
    public List<EnvironmentCompatibilityOutput> list(String lifecycle) {
        LifecycleTypeCode effectiveLifecycle =
            lifecycle == null || lifecycle.isBlank() ? LifecycleTypeCode.active() : LifecycleTypeCode.of(lifecycle);
        return repository
            .findByLifecycle(effectiveLifecycle)
            .stream()
            .map(EnvironmentCompatibilityOutput::from)
            .toList();
    }
}
