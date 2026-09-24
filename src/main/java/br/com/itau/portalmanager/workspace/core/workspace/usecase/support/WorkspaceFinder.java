package br.com.itau.portalmanager.workspace.core.workspace.usecase.support;

import br.com.itau.portalmanager.workspace.core.workspace.domain.LifecycleTypeCode;
import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.itau.portalmanager.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceFinder {

    private final WorkspaceRepository repository;

    public WorkspaceFinder(WorkspaceRepository repository) {
        this.repository = repository;
    }

    public Workspace findActive(String identifier) {
        return repository.findByIdentifierAndLifecycleValue(identifier, LifecycleTypeCode.active().value())
                .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }

    public Workspace findInactiveForRestore(String identifier) {
        return repository.findByIdentifierAndLifecycleValue(identifier, LifecycleTypeCode.inactive().value())
                .orElseThrow(() -> new ValidationException(WorkspaceMessageKeys.RESTORE_INVALID));
    }

    public Workspace findInactiveForDeletion(String identifier) {
        Workspace workspace = repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));

        if (!LifecycleTypeCode.inactive().equals(workspace.getLifecycle())) {
            throw new ValidationException(WorkspaceMessageKeys.DELETE_INVALID);
        }

        return workspace;
    }
}
