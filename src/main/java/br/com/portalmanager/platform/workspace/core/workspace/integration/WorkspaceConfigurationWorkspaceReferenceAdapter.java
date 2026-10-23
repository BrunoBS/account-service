package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.WorkspaceReferencePort;

import org.springframework.stereotype.Component;

@Component
public class WorkspaceConfigurationWorkspaceReferenceAdapter implements WorkspaceReferencePort {

    private final WorkspaceFinder workspaceFinder;

    public WorkspaceConfigurationWorkspaceReferenceAdapter(WorkspaceFinder workspaceFinder) {
        this.workspaceFinder = workspaceFinder;
    }

    @Override
    public ResolvedWorkspace resolveActive(String workspaceIdentifier) {
        WorkspaceReference workspace = workspaceFinder.findActiveReference(workspaceIdentifier);
        return new ResolvedWorkspace(
                workspace.getId(), workspace.getIdentifier(), workspace.getWorkspaceType());
    }

    @Override
    public ResolvedWorkspace resolveActiveForUpdate(String workspaceIdentifier) {
        Workspace workspace = workspaceFinder.findActiveForUpdate(workspaceIdentifier);
        return new ResolvedWorkspace(
                workspace.getId(), workspace.getIdentifier(), workspace.getWorkspaceType().value());
    }
}
