package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.environmenttype.EnvironmentTypeMapper;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnvironmentTypeQueryService {

    private final EnvironmentTypeRepository types;
    private final EnvironmentTypeFinder finder;
    private final EnvironmentTypeMapper mapper;

    public EnvironmentTypeQueryService(
            EnvironmentTypeRepository types,
            EnvironmentTypeFinder finder,
            EnvironmentTypeMapper mapper) {
        this.types = types;
        this.finder = finder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EnvironmentTypeOutput find(String code) {
        return mapper.toOutput(finder.find(code));
    }

    @Transactional(readOnly = true)
    public List<EnvironmentTypeOutput> list(String lifecycle) {
        return types
                .findByLifecycleOrderByDisplayOrderAscCodeAsc(LifecycleFilter.resolve(lifecycle))
                .stream()
                .map(mapper::toOutput)
                .toList();
    }
}
