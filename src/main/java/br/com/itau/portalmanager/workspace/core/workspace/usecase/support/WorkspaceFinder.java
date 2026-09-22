package br.com.itau.portalmanager.workspace.core.workspace.usecase.support;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.itau.portalmanager.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.lifecycle.LifecycleTypeEnum;
import br.com.portalmanager.platform.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceFinder {

    private final WorkspaceRepository repository;

    public WorkspaceFinder(WorkspaceRepository repository) {
        this.repository = repository;
    }

    public Workspace findActive(Long id) {
        return repository.findByIdAndLifecycle(id, LifecycleTypeEnum.ACTIVE)
                .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }

    public Workspace findInactiveForRestore(Long id) {
        return repository.findByIdAndLifecycle(id, LifecycleTypeEnum.INACTIVE)
                .orElseThrow(() -> new ValidationException(WorkspaceMessageKeys.RESTORE_INVALID));
    }
}
