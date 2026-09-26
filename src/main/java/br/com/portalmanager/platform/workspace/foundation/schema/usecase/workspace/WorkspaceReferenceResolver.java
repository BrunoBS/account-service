package br.com.portalmanager.platform.workspace.foundation.schema.usecase.workspace;

public interface WorkspaceReferenceResolver {

    Long resolveInternalId(String workspaceIdentifier);

    Long resolveActiveInternalId(String workspaceIdentifier);

    String resolveIdentifier(Long workspaceId);
}
