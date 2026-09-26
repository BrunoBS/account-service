package br.com.portalmanager.platform.workspace.core.workspace.usecase.support;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceFinder {
    private final WorkspaceRepository repository;
    public WorkspaceFinder(WorkspaceRepository repository) { this.repository=repository; }

    public Workspace findActive(String identifier) {
        return repository.findByIdentifierAndLifecycleValue(identifier, LifecycleTypeCode.active().value())
                .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }

    public Workspace findActive(Long id) {
        return repository.findByIdAndLifecycleValue(id, LifecycleTypeCode.active().value())
                .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }

    public Workspace findInactiveForRestore(String identifier) {
        return repository.findByIdentifierAndLifecycleValue(identifier, LifecycleTypeCode.inactive().value())
                .orElseThrow(() -> new ValidationException(WorkspaceMessageKeys.RESTORE_INVALID));
    }

    public Workspace findInactiveForDeletion(String identifier) {
        Workspace workspace=repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
        if (!LifecycleTypeCode.inactive().equals(workspace.getLifecycle())) {
            throw new ValidationException(WorkspaceMessageKeys.DELETE_INVALID);
        }
        return workspace;
    }
}
