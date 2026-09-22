package br.com.itau.portalmanager.workspace.core.workspace.usecase.findbyid;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceFinder;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceTaggingSupport;
import br.com.portalmanager.platform.authorization.annotation.ResourceVisibility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindWorkspaceByIdUseCase {

    private final WorkspaceFinder finder;
    private final WorkspaceTaggingSupport taggingSupport;

    public FindWorkspaceByIdUseCase(WorkspaceFinder finder, WorkspaceTaggingSupport taggingSupport) {
        this.finder = finder;
        this.taggingSupport = taggingSupport;
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public WorkspaceOutput execute(Long id) {
        Workspace workspace = finder.findActive(id);
        return WorkspaceOutput.from(workspace, taggingSupport.findManual(workspace));
    }
}
