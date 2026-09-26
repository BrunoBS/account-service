package br.com.portalmanager.platform.workspace.foundation.schema.integration;

public interface WorkspaceReferenceResolver {

    Long resolveInternalId(String workspaceIdentifier);

    String resolveIdentifier(Long workspaceId);
}
