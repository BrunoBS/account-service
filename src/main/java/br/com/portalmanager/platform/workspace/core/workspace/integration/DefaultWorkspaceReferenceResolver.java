package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkspaceReferenceResolver implements WorkspaceReferenceResolver {

    private final WorkspaceFinder workspaceFinder;

    public DefaultWorkspaceReferenceResolver(WorkspaceFinder workspaceFinder) {
        this.workspaceFinder = workspaceFinder;
    }

    @Override
    public Long resolveInternalId(String workspaceIdentifier) {
        Workspace workspace = workspaceFinder.findByIdentifier(workspaceIdentifier);
        br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator.requireActive(workspace);
        return workspace.getId();
    }

    @Override
    public String resolveIdentifier(Long workspaceId) {
        Workspace workspace = workspaceFinder.findById(workspaceId);
        br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator.requireActive(workspace);
        return workspace.getIdentifier();
    }

    @Override
    public String resolveWorkspaceType(String workspaceIdentifier) {
        Workspace workspace = workspaceFinder.findByIdentifier(workspaceIdentifier);
        br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator.requireActive(workspace);
        return workspace.getWorkspaceType().value();
    }
}
