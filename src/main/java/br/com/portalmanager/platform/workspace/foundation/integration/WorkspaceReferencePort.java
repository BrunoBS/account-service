package br.com.portalmanager.platform.workspace.foundation.integration;

public interface WorkspaceReferencePort {
    ResolvedWorkspace resolveActive(String workspaceIdentifier);

    ResolvedWorkspace resolveActiveForUpdate(String workspaceIdentifier);

    Long resolveInternalId(String workspaceIdentifier);

    String resolveIdentifier(Long workspaceId);

    record ResolvedWorkspace(Long id, String identifier, String workspaceType) {}
}
