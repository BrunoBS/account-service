package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceFinder {
    private final WorkspaceRepository repository;

    public WorkspaceFinder(WorkspaceRepository repository) {
        this.repository = repository;
    }

    public Workspace findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }

    public Workspace findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }
}
