package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentTypeQueryService {

    private final EnvironmentTypeRepository types;

    public EnvironmentTypeQueryService(EnvironmentTypeRepository types) {
        this.types = types;
    }

    @Transactional(readOnly = true)
    public EnvironmentType active(String code) {
        return types
            .findByCodeAndLifecycle(code, LifecycleTypeCode.active())
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.TYPE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public EnvironmentType find(String identifier) {
        return types
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.TYPE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public EnvironmentTypeOutput findOutput(String identifier) {
        return EnvironmentTypeOutput.from(find(identifier));
    }

    @Transactional(readOnly = true)
    public List<EnvironmentTypeOutput> list() {
        return types.findAllByOrderByDisplayOrderAscIdAsc().stream().map(EnvironmentTypeOutput::from).toList();
    }
}
