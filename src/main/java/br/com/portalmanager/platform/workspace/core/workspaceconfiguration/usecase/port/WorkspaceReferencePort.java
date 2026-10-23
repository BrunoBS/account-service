package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port;

public interface WorkspaceReferencePort {
    ResolvedWorkspace resolveActive(String workspaceIdentifier);

    ResolvedWorkspace resolveActiveForUpdate(String workspaceIdentifier);

    record ResolvedWorkspace(Long id, String identifier, String workspaceType) {}
}
