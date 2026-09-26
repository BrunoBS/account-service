package br.com.itau.portalmanager.workspace.foundation.schema.usecase.workspace;

public interface WorkspaceReferenceResolver {

    Long resolveInternalId(String workspaceIdentifier);

    Long resolveActiveInternalId(String workspaceIdentifier);

    String resolveIdentifier(Long workspaceId);
}
