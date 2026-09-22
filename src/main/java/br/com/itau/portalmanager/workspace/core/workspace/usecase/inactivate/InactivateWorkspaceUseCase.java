package br.com.itau.portalmanager.workspace.core.workspace.usecase.inactivate;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceFinder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InactivateWorkspaceUseCase {

    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;

    public InactivateWorkspaceUseCase(WorkspaceRepository repository, WorkspaceFinder finder) {
        this.repository = repository;
        this.finder = finder;
    }

    @Transactional
    public void execute(Long id) {
        Workspace workspace = finder.findActive(id);
        workspace.inactivate(LocalDateTime.now());
        repository.saveAndFlush(workspace);
    }
}
