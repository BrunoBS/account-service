package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentTypeQueryService {

    private final EnvironmentTypeRepository types;
    private final EnvironmentTypeFinder finder;

    public EnvironmentTypeQueryService(EnvironmentTypeRepository types, EnvironmentTypeFinder finder) {
        this.types = types;
        this.finder = finder;
    }

    @Transactional(readOnly = true)
    public EnvironmentTypeOutput find(String code) {
        return EnvironmentTypeOutput.from(finder.find(code));
    }

    @Transactional(readOnly = true)
    public List<EnvironmentTypeOutput> list(String lifecycle) {
        return types
            .findByLifecycleOrderByDisplayOrderAscCodeAsc(LifecycleFilter.resolve(lifecycle))
            .stream()
            .map(EnvironmentTypeOutput::from)
            .toList();
    }
}
