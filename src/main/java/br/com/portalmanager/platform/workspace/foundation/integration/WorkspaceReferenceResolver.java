package br.com.portalmanager.platform.workspace.foundation.integration;

public interface WorkspaceReferenceResolver {
    Long resolveInternalId(String workspaceIdentifier);

    String resolveIdentifier(Long workspaceId);

    String resolveWorkspaceType(String workspaceIdentifier);
}
