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
        Workspace workspace = workspaceFinder.findActive(workspaceIdentifier);
        return workspace.getId();
    }

    @Override
    public String resolveIdentifier(Long workspaceId) {
        Workspace workspace = workspaceFinder.findActive(workspaceId);
        return workspace.getIdentifier();
    }
}
