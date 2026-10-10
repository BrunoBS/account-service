package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceFinder {

    private final WorkspaceRepository repository;

    public WorkspaceFinder(WorkspaceRepository repository) {
        this.repository = repository;
    }

    public Workspace findActive(String identifier) {
        Workspace workspace = repository
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
        WorkspaceValidator.requireActive(workspace);
        return workspace;
    }

    public Workspace findActive(Long id) {
        Workspace workspace = repository
            .findById(id)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
        WorkspaceValidator.requireActive(workspace);
        return workspace;
    }

    public Workspace findInactiveForRestore(String identifier) {
        Workspace workspace = repository
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
        WorkspaceValidator.requireRestorable(workspace);
        return workspace;
    }

    public Workspace findInactiveForDeletion(String identifier) {
        Workspace workspace = repository
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
        WorkspaceValidator.requireDeletable(workspace);
        return workspace;
    }
}
