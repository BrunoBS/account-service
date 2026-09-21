package br.com.itau.portalmanager.workspace.core.workspace.usecase.restore;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceFinder;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceTaggingSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestoreWorkspaceUseCase {

    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceTaggingSupport taggingSupport;

    public RestoreWorkspaceUseCase(
            WorkspaceRepository repository,
            WorkspaceFinder finder,
            WorkspaceTaggingSupport taggingSupport
    ) {
        this.repository = repository;
        this.finder = finder;
        this.taggingSupport = taggingSupport;
    }

    @Transactional
    public WorkspaceOutput execute(Long id) {
        Workspace workspace = finder.findInactiveForRestore(id);
        List<String> manualTags = taggingSupport.findManual(workspace);

        workspace.restore(LocalDateTime.now());
        Workspace saved = repository.saveAndFlush(workspace);
        taggingSupport.reconcile(saved, manualTags);
        return WorkspaceOutput.from(saved, taggingSupport.findManual(saved));
    }
}
