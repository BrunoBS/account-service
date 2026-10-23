package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.environment.usecase.port.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;

import org.springframework.stereotype.Component;

@Component
public class EnvironmentWorkspaceReferenceAdapter implements WorkspaceReferencePort {

    private final WorkspaceFinder workspaceFinder;

    public EnvironmentWorkspaceReferenceAdapter(WorkspaceFinder workspaceFinder) {
        this.workspaceFinder = workspaceFinder;
    }

    @Override
    public Long resolveActiveForUpdate(String identifier) {
        return workspaceFinder.findActiveForUpdate(identifier).getId();
    }

    @Override
    public Long resolveInternalId(String workspaceIdentifier) {
        WorkspaceReference workspace = workspaceFinder.findActiveReference(workspaceIdentifier);
        return workspace.getId();
    }
}
